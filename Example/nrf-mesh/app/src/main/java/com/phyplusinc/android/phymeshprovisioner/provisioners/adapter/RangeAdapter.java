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

package com.phyplusinc.android.phymeshprovisioner.provisioners.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.AddressRange;
import no.nordicsemi.android.meshprovisioner.AllocatedGroupRange;
import no.nordicsemi.android.meshprovisioner.AllocatedSceneRange;
import no.nordicsemi.android.meshprovisioner.AllocatedUnicastRange;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.Range;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.widgets.RangeView;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

public class RangeAdapter extends RecyclerView.Adapter<RangeAdapter.ViewHolder> {

    private final ArrayList<Range> mRanges;
    private final List<Provisioner> mProvisioners;
    private final Context mContext;
    private final String mUuid;
    private OnItemClickListener mOnItemClickListener;

    public RangeAdapter(@NonNull final Context context, @NonNull final String uuid, @NonNull final List<? extends Range> ranges, @NonNull final List<Provisioner> provisioners) {
        mContext = context;
        mUuid = uuid;
        mRanges = new ArrayList<>(ranges);
        mProvisioners = provisioners;
    }

    public void setOnItemClickListener(final RangeAdapter.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void updateData(@NonNull List<? extends Range> ranges) {
        mRanges.clear();
        mRanges.addAll(ranges);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RangeAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View layoutView = LayoutInflater.from(mContext).inflate(R.layout.range_item, parent, false);
        return new RangeAdapter.ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final RangeAdapter.ViewHolder holder, final int position) {
        final Range range = mRanges.get(position);
        final String low, high;
        if (range instanceof AddressRange) {
            low = MeshAddress.formatAddress(((AddressRange) range).getLowAddress(), true);
            high = MeshAddress.formatAddress(((AddressRange) range).getHighAddress(), true);
        } else {
            low = MeshAddress.formatAddress(((AllocatedSceneRange) range).getFirstScene(), true);
            high = MeshAddress.formatAddress(((AllocatedSceneRange) range).getLastScene(), true);
        }
        holder.rangeValue.setText(mContext.getString(R.string.range_adapter_format, low, high));
        holder.rangeView.clearRanges();
        holder.rangeView.addRange(range);
        addOverlappingRanges(range, holder.rangeView);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mRanges.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public Range getItem(final int position) {
        return mRanges.get(position);
    }

    public List<Range> getItems() {
        return mRanges;
    }

    public void addItem(final int position, @NonNull final Range range) {
        mRanges.add(position, range);
        notifyItemInserted(position);
    }

    public void removeItem(final int position) {
        mRanges.remove(position);
        notifyItemRemoved(position);
    }

    private void addOverlappingRanges(@NonNull final Range range, @NonNull final RangeView rangeView) {
        rangeView.clearOtherRanges();
        for (Provisioner p : mProvisioners) {
            if (!p.getProvisionerUuid().equalsIgnoreCase(mUuid)) {
                if (range instanceof AllocatedUnicastRange) {
                    for (AllocatedUnicastRange otherRange : p.getAllocatedUnicastRanges()) {
                        if (range.overlaps(otherRange)) {
                            rangeView.addOtherRange(otherRange);
                        }
                    }
                } else if (range instanceof AllocatedGroupRange) {
                    for (AllocatedGroupRange otherRange : p.getAllocatedGroupRanges()) {
                        if (range.overlaps(otherRange)) {
                            rangeView.addOtherRange(otherRange);
                        }
                    }
                } else {
                    for (AllocatedSceneRange otherRange : p.getAllocatedSceneRanges()) {
                        if (range.overlaps(otherRange)) {
                            rangeView.addOtherRange(otherRange);
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface OnItemClickListener {
        void onItemClick(final int position, @NonNull final Range range);
    }

    final class ViewHolder extends RemovableViewHolder {

        @BindView(R.id.range_text)
        TextView rangeValue;
        @BindView(R.id.range)
        RangeView rangeView;

        private ViewHolder(final View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.findViewById(R.id.removable).setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(getAdapterPosition(), mRanges.get(getAdapterPosition()));
                }
            });
        }
    }
}
