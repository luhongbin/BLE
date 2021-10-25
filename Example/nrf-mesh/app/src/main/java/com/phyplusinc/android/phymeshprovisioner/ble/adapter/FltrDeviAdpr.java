/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.phyplusinc.android.phymeshprovisioner.ble.adapter;

import android.content.Context;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.ExtendedBluetoothDevice;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.ScannerLiveData;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;

public class FltrDeviAdpr extends RecyclerView.Adapter<FltrDeviAdpr.ViewHolder> {
    private Context mContext;
    private final List<ExtendedBluetoothDevice> mDevices;
    private final List<ExtendedBluetoothDevice> mDispDevi;
    private OnItemClickListener mOnItemClickListener;
    private final UUID mFltr;

    public FltrDeviAdpr(final FragmentActivity fragmentActivity, final ScannerLiveData scannerLiveData, final UUID fltr) {
        mContext = fragmentActivity;
        mDevices = scannerLiveData.getDevices();
        mDispDevi = new LinkedList<>();
        mFltr = fltr;
        scannerLiveData.observe(fragmentActivity, devices -> {
            mDispDevi.clear();
            for ( ExtendedBluetoothDevice devi : mDevices ) {
                final ScanRecord scan = devi.getScanResult().getScanRecord();
                if (null != scan) {
                    final List<ParcelUuid> uuids = scan.getServiceUuids();
                    if (null != uuids) {
                        for (ParcelUuid uuid : uuids) {
                            if (uuid.getUuid().equals(mFltr)) {
                                mDispDevi.add(devi);
                                break;
                            }
                        }
                    }
                }
            }
            notifyDataSetChanged();
        });
    }

//    public FltrDeviAdpr(final FragmentActivity fragmentActivity, final ScannerLiveData scannerLiveData, final UUID fltr) {
//        mContext = fragmentActivity;
//        mDevices = scannerLiveData.getDevices();
//        mDispDevi = new LinkedList<>();
//        mFltr = fltr;
//        scannerLiveData.observe(fragmentActivity, devices -> {
//            Integer indx = devices.getUpdatedDeviceIndex();
//
//            if (indx == null) {
//                mDispDevi.clear();
//                for ( ExtendedBluetoothDevice devi : mDevices ) {
//                    final ScanRecord scan = devi.getScanResult().getScanRecord();
//                    if (null != scan) {
//                        final List<ParcelUuid> uuids = scan.getServiceUuids();
//                        if (null != uuids) {
//                            for (ParcelUuid uuid : uuids) {
//                                if (uuid.getUuid().equals(mFltr)) {
//                                    mDispDevi.add(devi);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//                notifyDataSetChanged();
//            } else {
//                {
//                    ExtendedBluetoothDevice devi = devices.getDevices().get(indx);
//                    final ScanRecord scan = devi.getScanResult().getScanRecord();
//                    if (null != scan) {
//                        final List<ParcelUuid> uuids = scan.getServiceUuids();
//                        if (null != uuids) {
//                            for (ParcelUuid uuid : uuids) {
//                                if (uuid.getUuid().equals(mFltr)) {
//                                    for (ExtendedBluetoothDevice disp : mDispDevi) {
//                                        if ( disp.getAddress().equals(devi.getAddress()) ) {
//                                            indx = mDispDevi.indexOf(disp);
//                                            disp.setRssi(devi.getRssi());
//                                            notifyItemChanged(indx);
//                                            break;
//                                        }
//                                    }
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        });
//    }

    public void setOnItemClickListener(@NonNull final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View layoutView = LayoutInflater.from(mContext).inflate(R.layout.fltr_devi_item, parent, false);
        return new ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final ExtendedBluetoothDevice device = mDispDevi.get(position);
        final String deviceName = device.getName();

        if (!TextUtils.isEmpty(deviceName))
            holder.deviceName.setText(deviceName);
        else
            holder.deviceName.setText(R.string.unknown_device);
        holder.deviceAddress.setText(device.getAddress());
        final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
        holder.rssi.setImageLevel(rssiPercent);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDispDevi.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(final ExtendedBluetoothDevice device);
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.device_address)
        TextView deviceAddress;
        @BindView(R.id.device_name)
        TextView deviceName;
        @BindView(R.id.rssi)
        ImageView rssi;

        private ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);

            view.findViewById(R.id.device_container).setOnClickListener(v -> {
                Log.d("PANDA", "ViewHolder: " + getAdapterPosition());
                if (mOnItemClickListener != null) {
                    if(getAdapterPosition() > -1) {
                        mOnItemClickListener.onItemClick(mDispDevi.get(getAdapterPosition()));
                    }
                }
            });
        }
    }
}
