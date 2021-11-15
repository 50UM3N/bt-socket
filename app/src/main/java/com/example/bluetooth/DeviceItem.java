package com.example.bluetooth;

public class DeviceItem {
    private final int mImage;
    private final String mDeviceName;
    private final String mDeviceMac;

    public DeviceItem(int image, String deviceName, String deviceMac) {
        mImage = image;
        mDeviceMac = deviceMac;
        mDeviceName = deviceName;
    }

    public int getImage() {
        return mImage;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public String getDeviceMac() {
        return mDeviceMac;
    }
}
