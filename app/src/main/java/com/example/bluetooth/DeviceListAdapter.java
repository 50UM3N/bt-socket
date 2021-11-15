package com.example.bluetooth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder> {
    private ArrayList<DeviceItem> mDeviceList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public TextView mDeviceName;
        public TextView mDeviceMac;

        public DeviceListViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.item_image);
            mDeviceName = itemView.findViewById(R.id.item_name);
            mDeviceMac = itemView.findViewById(R.id.item_mac);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public DeviceListAdapter(ArrayList<DeviceItem> deviceList) {
        mDeviceList = deviceList;
    }

    @NonNull
    @Override
    public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        DeviceListViewHolder dlvh = new DeviceListViewHolder(v, mListener);
        return dlvh;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewHolder holder, int position) {
        DeviceItem currentDevice = mDeviceList.get(position);

        holder.mImageView.setImageResource(currentDevice.getImage());
        holder.mDeviceName.setText(currentDevice.getDeviceName());
        holder.mDeviceMac.setText(currentDevice.getDeviceMac());
    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }
}
