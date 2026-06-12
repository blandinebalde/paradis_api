package com.paradissaveurs.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shop_settings")
public class ShopSettingsEntity {

    @Id
    private Long id = 1L;

    private Integer maxProductsPerOrder;
    private String shopName;
    private String slogan;
    private String shopAddress;
    private String shopPhone;
    private String waveNumber;
    private String omNumber;
    private String adminNotifyPhone;
    private Boolean notifyViaWhatsApp;
    private Boolean notifyViaSms;
    private Boolean wave;
    private Boolean om;
    private Boolean cash;

    @Column(length = 1000)
    private String categoriesJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMaxProductsPerOrder() { return maxProductsPerOrder; }
    public void setMaxProductsPerOrder(Integer maxProductsPerOrder) { this.maxProductsPerOrder = maxProductsPerOrder; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }
    public String getShopAddress() { return shopAddress; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }
    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }
    public String getWaveNumber() { return waveNumber; }
    public void setWaveNumber(String waveNumber) { this.waveNumber = waveNumber; }
    public String getOmNumber() { return omNumber; }
    public void setOmNumber(String omNumber) { this.omNumber = omNumber; }
    public String getAdminNotifyPhone() { return adminNotifyPhone; }
    public void setAdminNotifyPhone(String adminNotifyPhone) { this.adminNotifyPhone = adminNotifyPhone; }
    public Boolean getNotifyViaWhatsApp() { return notifyViaWhatsApp; }
    public void setNotifyViaWhatsApp(Boolean notifyViaWhatsApp) { this.notifyViaWhatsApp = notifyViaWhatsApp; }
    public Boolean getNotifyViaSms() { return notifyViaSms; }
    public void setNotifyViaSms(Boolean notifyViaSms) { this.notifyViaSms = notifyViaSms; }
    public Boolean getWave() { return wave; }
    public void setWave(Boolean wave) { this.wave = wave; }
    public Boolean getOm() { return om; }
    public void setOm(Boolean om) { this.om = om; }
    public Boolean getCash() { return cash; }
    public void setCash(Boolean cash) { this.cash = cash; }
    public String getCategoriesJson() { return categoriesJson; }
    public void setCategoriesJson(String categoriesJson) { this.categoriesJson = categoriesJson; }
}
