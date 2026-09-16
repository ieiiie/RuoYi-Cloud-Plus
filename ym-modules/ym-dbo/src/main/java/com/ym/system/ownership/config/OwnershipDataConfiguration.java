package com.ym.system.ownership.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;

/** Resolve the physical IoT datasource explicitly; neither @DS nor a caller's transaction may redirect it. */
@Configuration(proxyBeanMethods = false)
public class OwnershipDataConfiguration {
    @Bean("deviceOwnershipJdbc")
    JdbcTemplate ownershipJdbc(DataSource dataSource,
        @Value("${ym.dbo.ownership.datasource:iot}") String name) {
        if (!"iot".equals(name)) throw new IllegalStateException("DBO ownership must use the explicit iot datasource");
        DataSource physical = dataSource;
        if (dataSource instanceof DynamicRoutingDataSource routing) {
            physical = routing.getDataSources().get(name);
            if (physical == null) throw new IllegalStateException("Missing ownership datasource: " + name);
        } else {
            throw new IllegalStateException("Named ownership datasource requires DynamicRoutingDataSource");
        }
        return new JdbcTemplate(physical);
    }

    @Bean("ownershipTenantJdbc")
    JdbcTemplate tenantJdbc(DataSource dataSource) {
        if (!(dataSource instanceof DynamicRoutingDataSource routing)
            || !routing.getDataSources().containsKey("saas"))
            throw new IllegalStateException("Missing SaaS datasource: saas");
        return new JdbcTemplate(routing.getDataSources().get("saas"));
    }
    @Bean("deviceOwnershipTransaction")
    TransactionTemplate ownershipTransaction(@Qualifier("deviceOwnershipJdbc") JdbcTemplate jdbc) {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(30);
        return tx;
    }
}
