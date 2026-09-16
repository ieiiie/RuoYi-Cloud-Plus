package com.ym.agriculture.farmtask.inventory.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Allocation;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AllocationLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetDevice;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetType;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetUsageLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Balance;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.BusinessSequence;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Idempotency;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.InboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Ledger;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.LegacyMaterialMap;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.Material;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialCategory;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceiptLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OperationLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.OutboundOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.ReturnOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeLine;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.StocktakeOrder;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.TaskMaterial;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategoryCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/** 将库存子域的小型 MyBatis-Plus Mapper 集中声明，避免为纯 CRUD 接口制造重复样板。 */
public final class InventoryMappers {

    @Mapper
    public interface MaterialCategoryMapper extends BaseMapperPlus<MaterialCategory, MaterialCategory> {
        default MaterialCategory selectTenantById(String tenantId, Long id) {
            return selectOne(Wrappers.<MaterialCategory>lambdaQuery()
                .eq(MaterialCategory::getTenantId, tenantId)
                .eq(MaterialCategory::getCategoryId, id));
        }
    }

    @Mapper
    public interface MaterialMapper extends BaseMapperPlus<Material, Material> {
        @Select("""
            SELECT category_id AS categoryId, COUNT(*) AS materialCount
            FROM sf_inventory_material
            WHERE tenant_id = #{tenantId}
              AND del_flag = '0'
              AND category_id IS NOT NULL
            GROUP BY category_id
            """)
        List<MaterialCategoryCount> selectCategoryCounts(@Param("tenantId") String tenantId);

        default Material selectTenantById(String tenantId, Long id) {
            return selectOne(Wrappers.<Material>lambdaQuery()
                .eq(Material::getTenantId, tenantId).eq(Material::getMaterialId, id)
                .eq(Material::getDelFlag, "0"));
        }
    }

    @Mapper
    public interface BalanceMapper extends BaseMapperPlus<Balance, Balance> {
        default Balance selectForUpdate(String tenantId, Long materialId) {
            return selectOne(Wrappers.<Balance>lambdaQuery()
                .eq(Balance::getTenantId, tenantId).eq(Balance::getMaterialId, materialId)
                .last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface LedgerMapper extends BaseMapperPlus<Ledger, Ledger> {
    }

    @Mapper
    public interface InboundOrderMapper extends BaseMapperPlus<InboundOrder, InboundOrder> {
        default InboundOrder selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<InboundOrder>lambdaQuery()
                .eq(InboundOrder::getTenantId, tenantId).eq(InboundOrder::getInboundOrderId, id)
                .last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface InboundLineMapper extends BaseMapperPlus<InboundLine, InboundLine> {
    }

    @Mapper
    public interface OutboundOrderMapper extends BaseMapperPlus<OutboundOrder, OutboundOrder> {
        default OutboundOrder selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<OutboundOrder>lambdaQuery()
                .eq(OutboundOrder::getTenantId, tenantId).eq(OutboundOrder::getOutboundOrderId, id)
                .last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface OutboundLineMapper extends BaseMapperPlus<OutboundLine, OutboundLine> {
    }

    @Mapper
    public interface ReturnOrderMapper extends BaseMapperPlus<ReturnOrder, ReturnOrder> {
        default ReturnOrder selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<ReturnOrder>lambdaQuery()
                .eq(ReturnOrder::getTenantId, tenantId).eq(ReturnOrder::getReturnOrderId, id)
                .last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface ReturnLineMapper extends BaseMapperPlus<ReturnLine, ReturnLine> {
    }

    @Mapper
    public interface StocktakeOrderMapper extends BaseMapperPlus<StocktakeOrder, StocktakeOrder> {
    }

    @Mapper
    public interface StocktakeLineMapper extends BaseMapperPlus<StocktakeLine, StocktakeLine> {
    }

    @Mapper
    public interface TaskMaterialMapper extends BaseMapperPlus<TaskMaterial, TaskMaterial> {
    }

    @Mapper
    public interface AllocationMapper extends BaseMapperPlus<Allocation, Allocation> {
    }

    @Mapper
    public interface AllocationLineMapper extends BaseMapperPlus<AllocationLine, AllocationLine> {
    }

    @Mapper
    public interface MaterialReceiptMapper extends BaseMapperPlus<MaterialReceipt, MaterialReceipt> {
        default MaterialReceipt selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
                .eq(MaterialReceipt::getTenantId, tenantId)
                .eq(MaterialReceipt::getMaterialReceiptId, id).last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface MaterialReceiptLineMapper extends BaseMapperPlus<MaterialReceiptLine, MaterialReceiptLine> {
    }

    @Mapper
    public interface AssetTypeMapper extends BaseMapperPlus<AssetType, AssetType> {
        default AssetType selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<AssetType>lambdaQuery()
                .eq(AssetType::getTenantId, tenantId).eq(AssetType::getAssetTypeId, id)
                .eq(AssetType::getDelFlag, "0").last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface AssetDeviceMapper extends BaseMapperPlus<AssetDevice, AssetDevice> {
        default AssetDevice selectForUpdate(String tenantId, Long id) {
            return selectOne(Wrappers.<AssetDevice>lambdaQuery()
                .eq(AssetDevice::getTenantId, tenantId).eq(AssetDevice::getAssetDeviceId, id)
                .eq(AssetDevice::getDelFlag, "0").last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface AssetUsageLogMapper extends BaseMapperPlus<AssetUsageLog, AssetUsageLog> {
    }

    @Mapper
    public interface OperationLogMapper extends BaseMapperPlus<OperationLog, OperationLog> {
    }

    @Mapper
    public interface BusinessSequenceMapper extends BaseMapperPlus<BusinessSequence, BusinessSequence> {
        default BusinessSequence selectForUpdate(String tenantId, String type, Date date) {
            return selectOne(Wrappers.<BusinessSequence>lambdaQuery()
                .eq(BusinessSequence::getTenantId, tenantId)
                .eq(BusinessSequence::getBusinessType, type)
                .eq(BusinessSequence::getSequenceDate, date).last("FOR UPDATE"));
        }
    }

    @Mapper
    public interface IdempotencyMapper extends BaseMapperPlus<Idempotency, Idempotency> {
        default Idempotency selectByScope(String tenantId, String operationType, String key) {
            return selectOne(Wrappers.<Idempotency>lambdaQuery()
                .eq(Idempotency::getTenantId, tenantId)
                .eq(Idempotency::getOperationType, operationType)
                .eq(Idempotency::getIdempotencyKey, key));
        }
    }

    @Mapper
    public interface LegacyMaterialMapMapper extends BaseMapperPlus<LegacyMaterialMap, LegacyMaterialMap> {
    }

    private InventoryMappers() {
    }
}
