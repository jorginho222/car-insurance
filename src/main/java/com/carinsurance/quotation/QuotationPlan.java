package com.carinsurance.quotation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One plan of a quotation: its coverage premiums, net premium and payment options. */
@Entity
@Table(name = "quotation_plan")
public class QuotationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Plan name;

    @ElementCollection
    @CollectionTable(name = "quotation_plan_coverage", joinColumns = @JoinColumn(name = "quotation_plan_id"))
    @OrderColumn(name = "position")
    private List<PlanCoverage> coverages = new ArrayList<>();

    @Column(name = "net_premium", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPremium;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_options", nullable = false, columnDefinition = "jsonb")
    private List<PaymentOption> paymentOptions = new ArrayList<>();

    protected QuotationPlan() {}

    public QuotationPlan(
            Plan name, List<PlanCoverage> coverages, BigDecimal netPremium, List<PaymentOption> paymentOptions) {
        this.name = name;
        this.coverages = new ArrayList<>(coverages);
        this.netPremium = netPremium;
        this.paymentOptions = new ArrayList<>(paymentOptions);
    }

    public Long getId() {
        return id;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    /** Only meant to be called by {@link Quotation#addPlan(QuotationPlan)}. */
    void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public Plan getName() {
        return name;
    }

    /** In the plan's coverage order. */
    public List<PlanCoverage> getCoverages() {
        return Collections.unmodifiableList(coverages);
    }

    public BigDecimal getNetPremium() {
        return netPremium;
    }

    /** One per installment option, fewest installments first. */
    public List<PaymentOption> getPaymentOptions() {
        return Collections.unmodifiableList(paymentOptions);
    }
}
