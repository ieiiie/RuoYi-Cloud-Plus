package com.ym.common.encrypt.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.encrypt.core.EncryptContextFactory;
import com.ym.common.encrypt.core.EncryptedFieldProcessor;
import com.ym.common.encrypt.core.EncryptorManager;
import com.ym.common.encrypt.enums.AlgorithmType;
import com.ym.common.encrypt.interceptor.MybatisDecryptInterceptor;
import com.ym.common.encrypt.interceptor.MybatisEncryptInterceptor;
import com.ym.common.encrypt.properties.EncryptorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 加解密配置
 *
 * @author 老马
 * @version 4.6.0
 */
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties({EncryptorProperties.class, MybatisPlusProperties.class})
@ConditionalOnClass(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(value = "mybatis-encryptor.enable", havingValue = "true")
public class EncryptorAutoConfiguration {

    @Autowired
    private EncryptorProperties properties;

    /**
     * 创建字段加解密管理器。
     *
     * @param mybatisPlusProperties MyBatis-Plus 配置
     * @return 字段加解密管理器
     */
    @Bean
    public EncryptorManager encryptorManager(MybatisPlusProperties mybatisPlusProperties) {
        validateEncryptorProperties(properties);
        return new EncryptorManager(mybatisPlusProperties.getTypeAliasesPackage());
    }

    /**
     * 创建加密上下文工厂。
     *
     * @return 加密上下文工厂
     */
    @Bean
    public EncryptContextFactory encryptContextFactory() {
        return new EncryptContextFactory(properties);
    }

    /**
     * 创建加密字段处理器。
     *
     * @param encryptorManager      加解密管理器
     * @param encryptContextFactory 加密上下文工厂
     * @return 加密字段处理器
     */
    @Bean
    public EncryptedFieldProcessor encryptedFieldProcessor(EncryptorManager encryptorManager, EncryptContextFactory encryptContextFactory) {
        return new EncryptedFieldProcessor(encryptorManager, encryptContextFactory);
    }

    /**
     * 创建 MyBatis 入参加密拦截器。
     *
     * @param encryptedFieldProcessor 加密字段处理器
     * @return MyBatis 入参加密拦截器
     */
    @Bean
    public MybatisEncryptInterceptor mybatisEncryptInterceptor(EncryptedFieldProcessor encryptedFieldProcessor) {
        return new MybatisEncryptInterceptor(encryptedFieldProcessor);
    }

    /**
     * 创建 MyBatis 出参解密拦截器。
     *
     * @param encryptedFieldProcessor 加密字段处理器
     * @return MyBatis 出参解密拦截器
     */
    @Bean
    public MybatisDecryptInterceptor mybatisDecryptInterceptor(EncryptedFieldProcessor encryptedFieldProcessor) {
        return new MybatisDecryptInterceptor(encryptedFieldProcessor);
    }

    private void validateEncryptorProperties(EncryptorProperties properties) {
        AlgorithmType algorithm = properties.getAlgorithm();
        if (algorithm == AlgorithmType.AES || algorithm == AlgorithmType.SM4) {
            if (StringUtils.isBlank(properties.getPassword())) {
                throw new IllegalArgumentException("mybatis-encryptor.password 不能为空");
            }
        }
        if (algorithm == AlgorithmType.RSA || algorithm == AlgorithmType.SM2) {
            if (StringUtils.isAnyBlank(properties.getPublicKey(), properties.getPrivateKey())) {
                throw new IllegalArgumentException("mybatis-encryptor.publicKey 与 privateKey 不能为空");
            }
        }
    }
}
