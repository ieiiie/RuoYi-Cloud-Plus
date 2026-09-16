package com.ym.agriculture.shared.i18n;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.I18nResourceResolution;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * stask 展示 VO 的维文替换器。
 *
 * <p>只替换通过注解显式登记的业务文本字段；没有成功译文或翻译查询失败时保留中文原值，
 * 保证既有 JSON 契约不变。</p>
 */
@Slf4j
@Component
public class StaskI18nResponseLocalizer {

    private static final String LOCALIZATION_FAILURE_METRIC = "stask.i18n.localization.failures";

    private final ISfI18nTextService i18nTextService;
    private final MeterRegistry meterRegistry;
    private final StaskMessageResolver messages;
    private final ConcurrentMap<Class<?>, TypeMetadata> metadataCache = new ConcurrentHashMap<>();

    /** 应用装配构造器。 */
    @Autowired
    public StaskI18nResponseLocalizer(ISfI18nTextService i18nTextService,
        ObjectProvider<MeterRegistry> meterRegistryProvider, StaskMessageResolver messages) {
        this.i18nTextService = i18nTextService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.messages = messages;
    }

    /** 单元测试兼容构造器。 */
    public StaskI18nResponseLocalizer(ISfI18nTextService i18nTextService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.i18nTextService = i18nTextService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.messages = null;
    }

    /** 单元测试兼容构造器。 */
    StaskI18nResponseLocalizer(ISfI18nTextService i18nTextService) {
        this.i18nTextService = i18nTextService;
        this.meterRegistry = null;
        this.messages = null;
    }

    /**
     * 将返回对象中显式绑定到翻译资源的展示文本替换为维文。
     *
     * @param tenantId 当前租户
     * @param body Controller 返回对象
     */
    public void localizeUyghur(String tenantId, Object body) {
        if (StringUtils.isBlank(tenantId) || body == null) {
            return;
        }
        List<TextBinding> bindings = new ArrayList<>();
        List<MessageBinding> messageBindings = new ArrayList<>();
        List<StaskI18nComposite> composites = new ArrayList<>();
        collect(body, new IdentityHashMap<>(), bindings, messageBindings, composites);
        if (bindings.isEmpty() && messageBindings.isEmpty()) {
            return;
        }
        LinkedHashSet<I18nTextSource> boundSources = bindings.stream()
            .flatMap(binding -> binding.sources().stream())
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        Map<String, I18nTextSource> sourceByText = new LinkedHashMap<>();
        boundSources.forEach(source -> sourceByText.putIfAbsent(source.sourceText(), source));
        Collection<I18nTextSource> sources = sourceByText.values();
        Map<String, String> translations = Map.of();
        try {
            if (!sources.isEmpty()) {
                I18nResourceResolution resolution = i18nTextService
                    .resolveAndRegisterMissingUyghurTextsByResources(tenantId, sources);
                Map<String, String> translationsByText = new LinkedHashMap<>();
                resolution.translations().forEach((source, translated) -> {
                    if (source != null && StringUtils.isNotBlank(translated)) {
                        translationsByText.putIfAbsent(source.sourceText(), translated);
                    }
                });
                translations = translationsByText;
                if (resolution.registeredCount() > 0) {
                    log.debug("stask i18n resources registered on demand: tenantId={}, sourceCount={}, registeredCount={}",
                        tenantId, sources.size(), resolution.registeredCount());
                }
            }
        } catch (RuntimeException exception) {
            log.warn("stask i18n localization lookup-or-register failed: tenantId={}, sourceCount={}, cause={}",
                tenantId, sources.size(), exception.getClass().getSimpleName());
            if (meterRegistry != null) {
                meterRegistry.counter(LOCALIZATION_FAILURE_METRIC,
                    "cause", exception.getClass().getSimpleName()).increment();
            }
            rebuildComposites(composites);
            localizeMessages(messageBindings);
            return;
        }
        if (translations != null && !translations.isEmpty()) {
            for (TextBinding binding : bindings) {
                String translated = firstTranslation(binding.sources(), translations);
                if (StringUtils.isNotBlank(translated)) {
                    set(binding, translated);
                }
            }
        }
        rebuildComposites(composites);
        localizeMessages(messageBindings);
    }

    private void collect(Object node, IdentityHashMap<Object, Boolean> visited, List<TextBinding> bindings,
        List<MessageBinding> messageBindings, List<StaskI18nComposite> composites) {
        if (node == null || visited.put(node, Boolean.TRUE) != null) {
            return;
        }
        if (node instanceof Collection<?> rows) {
            rows.forEach(row -> collect(row, visited, bindings, messageBindings, composites));
            return;
        }
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> collect(value, visited, bindings, messageBindings, composites));
            return;
        }
        Class<?> type = node.getClass();
        if (!type.getName().startsWith("com.ym.")) {
            return;
        }
        if (node instanceof StaskI18nComposite composite) {
            composites.add(composite);
        }
        TypeMetadata metadata = metadataCache.computeIfAbsent(type, this::inspect);
        for (FieldMetadata fieldMetadata : metadata.fields()) {
            Object value = get(node, fieldMetadata.field());
            if (value instanceof String text && StringUtils.isNotBlank(text)
                && !fieldMetadata.bindings().isEmpty()) {
                List<I18nTextSource> sources = sources(node, text, fieldMetadata.bindings());
                if (!sources.isEmpty()) {
                    bindings.add(new TextBinding(node, fieldMetadata.field(), null, sources));
                }
            }
            if (value instanceof String text && StringUtils.isNotBlank(text)
                && fieldMetadata.message() != null) {
                messageBindings.add(new MessageBinding(node, fieldMetadata.field(), fieldMetadata.message()));
            }
            collect(value, visited, bindings, messageBindings, composites);
        }
    }

    private static void rebuildComposites(List<StaskI18nComposite> composites) {
        for (int index = composites.size() - 1; index >= 0; index--) {
            composites.get(index).rebuildLocalizedText();
        }
    }

    private TypeMetadata inspect(Class<?> type) {
        Map<String, Field> fields = fields(type);
        List<FieldMetadata> metadata = new ArrayList<>(fields.size());
        for (Field field : fields.values()) {
            List<SourceMetadata> bindings = new ArrayList<>();
            for (StaskI18nField annotation : field.getAnnotationsByType(StaskI18nField.class)) {
                Field idField = fields.get(annotation.idProperty());
                if (idField == null) {
                    log.warn("stask i18n field binding ignored: type={}, field={}, idProperty={}",
                        type.getName(), field.getName(), annotation.idProperty());
                    continue;
                }
                bindings.add(new SourceMetadata(annotation.resourceType(), idField, annotation.fieldKey()));
            }
            MessageMetadata messageMetadata = null;
            StaskI18nMessage message = field.getAnnotation(StaskI18nMessage.class);
            if (message != null) {
                List<Field> argumentFields = new ArrayList<>();
                boolean valid = true;
                for (String property : message.argumentProperties()) {
                    Field argumentField = fields.get(property);
                    if (argumentField == null) {
                        log.warn("stask i18n message binding ignored: type={}, field={}, argumentProperty={}",
                            type.getName(), field.getName(), property);
                        valid = false;
                        break;
                    }
                    argumentFields.add(argumentField);
                }
                if (valid) {
                    messageMetadata = new MessageMetadata(message.key(), List.copyOf(argumentFields));
                }
            }
            metadata.add(new FieldMetadata(field, List.copyOf(bindings), messageMetadata));
        }
        return new TypeMetadata(List.copyOf(metadata));
    }

    private static List<I18nTextSource> sources(Object target, String text, List<SourceMetadata> metadata) {
        LinkedHashSet<I18nTextSource> sources = new LinkedHashSet<>();
        for (SourceMetadata source : metadata) {
            Long resourceId = longValue(target, source.idField());
            // 资源 ID 只用于业务定位，不再是词条持久化身份。遗留响应缺少稳定 ID 时，
            // 仍可按当前完整中文命中或懒登记 tenant 级唯一词条。
            sources.add(new I18nTextSource(
                source.resourceType(), resourceId, source.fieldKey(), text));
        }
        return List.copyOf(sources);
    }

    private static String firstTranslation(List<I18nTextSource> sources,
        Map<String, String> translations) {
        for (I18nTextSource source : sources) {
            String translated = translations.get(source.sourceText());
            if (StringUtils.isNotBlank(translated)) {
                return translated;
            }
        }
        return null;
    }

    private static Map<String, Field> fields(Class<?> type) {
        Map<String, Field> result = new LinkedHashMap<>();
        Class<?> current = type;
        while (current != null && current.getName().startsWith("com.ym.")) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers()) && field.trySetAccessible()) {
                    result.putIfAbsent(field.getName(), field);
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    private static Long longValue(Object target, Field field) {
        Object value = get(target, field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object get(Object target, Field field) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private void localizeMessages(List<MessageBinding> bindings) {
        if (messages == null || bindings.isEmpty()) {
            return;
        }
        for (MessageBinding binding : bindings) {
            Object[] arguments = binding.metadata().argumentFields().stream()
                .map(field -> get(binding.target(), field))
                .toArray();
            set(new TextBinding(binding.target(), binding.field(), null, List.of()),
                messages.message(binding.metadata().key(), arguments));
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean set(TextBinding binding, String value) {
        if (binding.field() == null && binding.target() instanceof Map<?, ?> map) {
            try {
                ((Map<String, Object>) map).put(binding.mapKey(), value);
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        try {
            binding.field().set(binding.target(), value);
            return true;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private record TypeMetadata(List<FieldMetadata> fields) {
    }

    private record FieldMetadata(Field field, List<SourceMetadata> bindings, MessageMetadata message) {
    }

    private record SourceMetadata(String resourceType, Field idField, String fieldKey) {
    }

    private record MessageMetadata(String key, List<Field> argumentFields) {
    }

    private record TextBinding(Object target, Field field, String mapKey, List<I18nTextSource> sources) {
    }

    private record MessageBinding(Object target, Field field, MessageMetadata metadata) {
    }
}
