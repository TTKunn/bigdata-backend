package com.example.bigdatabackend.dto;

/**
 * 订单收货地址DTO
 */
public class OrderAddressDto {

    private String receiver;
    private String phone;
    private String address;
    private String postcode;

    public OrderAddressDto() {
    }

    public OrderAddressDto(String receiver, String phone, String address, String postcode) {
        this.receiver = receiver;
        this.phone = phone;
        this.address = address;
        this.postcode = postcode;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }
}
