package com.ym.system.ownership.config;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import static org.junit.jupiter.api.Assertions.*;

class OwnershipDataConfigurationTest {
    @Test void resolvesPhysicalIotAndSaasAndNeverFallsBackToMaster() {
        var routing=new DynamicRoutingDataSource(java.util.List.of());
        var iot=new DriverManagerDataSource("jdbc:h2:mem:iot");
        var saas=new DriverManagerDataSource("jdbc:h2:mem:saas");
        routing.addDataSource("iot",iot); routing.addDataSource("saas",saas);
        var config=new OwnershipDataConfiguration();
        var jdbc=config.ownershipJdbc(routing,"iot");
        assertSame(iot,jdbc.getDataSource()); assertSame(saas,config.tenantJdbc(routing).getDataSource());
        assertThrows(IllegalStateException.class,()->config.ownershipJdbc(routing,"missing"));
        assertThrows(IllegalStateException.class,()->config.ownershipJdbc(iot,"iot"));
        var tx=config.ownershipTransaction(jdbc);
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW,tx.getPropagationBehavior());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED,tx.getIsolationLevel());
    }
    @Test void missingSaasFailsClosed() {
        assertThrows(IllegalStateException.class,()->new OwnershipDataConfiguration().tenantJdbc(new DynamicRoutingDataSource(java.util.List.of())));
    }
}
