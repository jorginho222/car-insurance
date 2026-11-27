package com.carinsurance.policy;

import com.carinsurance.insured.InsuredMapper;
import com.carinsurance.insured.InsuredVehicleMapper;
import com.carinsurance.location.LocalityMapper;
import com.carinsurance.policy.dto.PolicyResponse;
import com.carinsurance.quotation.Quotation;
import com.carinsurance.quotation.QuotationPlan;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

    private final InsuredMapper insuredMapper;
    private final InsuredVehicleMapper vehicleMapper;
    private final LocalityMapper localityMapper;

    public PolicyMapper(
            InsuredMapper insuredMapper, InsuredVehicleMapper vehicleMapper, LocalityMapper localityMapper) {
        this.insuredMapper = insuredMapper;
        this.vehicleMapper = vehicleMapper;
        this.localityMapper = localityMapper;
    }

    public PolicyResponse toResponse(Policy policy) {
        QuotationPlan plan = policy.getQuotationPlan();
        Quotation quotation = plan.getQuotation();
        return new PolicyResponse(
                policy.getId(),
                policy.getNumber(),
                policy.getValidFrom(),
                policy.getValidTo(),
                new PolicyResponse.Quotation(quotation.getId(), quotation.getCode()),
                new PolicyResponse.PolicyPlan(
                        plan.getId(),
                        plan.getName(),
                        // a copy, so the lazy collection is loaded here and not while writing the JSON
                        List.copyOf(plan.getCoverages()),
                        plan.getNetPremium()),
                policy.getPaymentOption(),
                policy.getPaymentMethod(),
                insuredMapper.toResponse(policy.getInsured()),
                vehicleMapper.toResponse(policy.getInsuredVehicle()),
                localityMapper.toResponse(policy.getLocality()),
                policy.getCreatedAt());
    }
}
