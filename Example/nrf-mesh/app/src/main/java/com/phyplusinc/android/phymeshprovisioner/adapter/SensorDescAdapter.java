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

package com.phyplusinc.android.phymeshprovisioner.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.transport.SensorDescStatus;
import no.nordicsemi.android.meshprovisioner.transport.SensorStatus;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class SensorDescAdapter extends RecyclerView.Adapter<SensorDescAdapter.ViewHolder> {

    private final Context mCtxt;
    private final MeshNetwork mNetw;
    private List<SensorDescStatus.DescInfo> mDesc = null;
    private Map mData = null;
    private OnItemClickListener mOnItemClickListener;


    public SensorDescAdapter(@NonNull final Context context, @NonNull final MeshNetwork network, @NonNull final List<SensorDescStatus.DescInfo> DescInfo, @NonNull final Map SnsrData) {
        this.mCtxt = context;
        this.mNetw = network;
        this.mDesc = DescInfo;
        this.mData = SnsrData;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(@NonNull final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public SensorDescAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View layoutView = LayoutInflater.from(mCtxt).inflate(R.layout.sensor_desc_item, parent, false);
        return new SensorDescAdapter.ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final SensorDescAdapter.ViewHolder holder, final int position) {
        if (null != mDesc && mDesc.size() > 0) {
            final int property = mDesc.get(position).getProperty();

            holder.name.setText(MeshAddress.formatAddress(property, true));
            holder.desc.setText(MeshAddress.formatAddress(property, true));

            final SensorStatus.SnsrData data = (SensorStatus.SnsrData) mData.get(property);
            if (null != data)
            holder.data.setText(MeshParserUtils.bytesToHex(data.getSnsrData(), true));

//            final int address = mDesc.get(position);
//            final Group group = network.getGroup(address);
//
//            if (group != null) {
//                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_group_black_24dp_alpha));
//                holder.address.setText(MeshAddress.formatAddress(address, true));
//            } else {
//                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_alpha_24dp));
//                holder.name.setText(mContext.getString(R.string.gkey_index, position+1));
//                holder.address.setText(R.string.address_unassigned);
//            }
        }
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mDesc.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    // @FunctionalInterface
    public interface OnItemClickListener {
        void onSetClicked(final int position);
    }

    public final class ViewHolder extends RemovableViewHolder {
        @BindView(R.id.container)
        FrameLayout container;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.tv_desc_name)
        TextView name;
        @BindView(R.id.tv_desc_numb)
        TextView desc;
        @BindView(R.id.tv_snsr_data)
        TextView data;

        private ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);

            container.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onSetClicked(getAdapterPosition());
                }
            });
        }
    }
}
