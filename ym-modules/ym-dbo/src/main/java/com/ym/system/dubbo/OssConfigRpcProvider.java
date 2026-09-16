package com.ym.system.dubbo;
import com.ym.jetlinks.oss.rpc.*;
import com.ym.system.mapper.SysOssConfigMapper;
import com.ym.system.domain.SysOssConfig;
import com.ym.common.json.utils.JsonUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** DBO 唯一配置源；此服务不上传文件，也不创建 sys_oss 记录。 */
@Component
@ConditionalOnProperty(name="jetlinks.oss.rpc-secret")
@DubboService(group="jetlinks-oss",version="1.0.0",retries=0,timeout=5000)
public class OssConfigRpcProvider implements OssConfigRpcService {
    private final SysOssConfigMapper mapper;private final String secret;
    private final ConcurrentHashMap<String,Long> nonces=new ConcurrentHashMap<>();
    public OssConfigRpcProvider(SysOssConfigMapper mapper,Environment env){this.mapper=mapper;this.secret=env.getRequiredProperty("jetlinks.oss.rpc-secret");}
    public String getDefaultConfig(String request){return read(null,request);}
    public String getConfigById(String id,String request){return read(id,request);}
    private String read(String id,String request){
        String expected=id==null?"default":id;
        String[] auth=OssRpcCipher.open(request,secret,"request").split("\\|",-1);
        long now=System.currentTimeMillis();
        if(auth.length!=4||!"jetlinks".equals(auth[0])||!expected.equals(auth[3]))throw new IllegalArgumentException("OSS配置调用认证失败");
        long at;try{at=Long.parseLong(auth[1]);}catch(Exception e){throw new IllegalArgumentException("OSS配置调用认证失败");}
        nonces.entrySet().removeIf(e->e.getValue()<now-120000);
        if(Math.abs(now-at)>60000||auth[2].length()!=36||nonces.size()>10000||nonces.putIfAbsent(auth[2],now)!=null)throw new IllegalArgumentException("OSS配置请求过期或重复");
        SysOssConfig c;
        if(id==null){var values=mapper.lambda().eq(SysOssConfig::getStatus,"Y").list();if(values.size()!=1)throw new IllegalArgumentException("OSS默认配置缺失或不唯一");c=values.get(0);}
        else {try{c=mapper.selectById(Long.valueOf(id));}catch(NumberFormatException e){throw new IllegalArgumentException("OSS配置标识无效");}}
        if(c==null)throw new IllegalArgumentException("OSS配置不存在，请恢复历史文件使用的配置");
        Map<String,Object> value=new LinkedHashMap<>();value.put("id",String.valueOf(c.getOssConfigId()));value.put("configKey",c.getConfigKey());
        value.put("endpoint",c.getEndpoint());value.put("domain",c.getDomainUrl());value.put("bucket",c.getBucketName());value.put("prefix",c.getPrefix());
        value.put("region",c.getRegion());value.put("https",c.getIsHttps());value.put("accessPolicy",c.getAccessPolicy());
        value.put("accessKey",c.getAccessKey());value.put("secretKey",c.getSecretKey());value.put("requestNonce",auth[2]);
        return OssRpcCipher.seal(JsonUtils.toJsonString(value),secret,"response");
    }
}
