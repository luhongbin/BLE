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

package com.phyplusinc.android.phymeshprovisioner.node.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.models.GenericOnOffServerModel;
import no.nordicsemi.android.meshprovisioner.models.LightHslServer;
import no.nordicsemi.android.meshprovisioner.models.SensorServer;
import no.nordicsemi.android.meshprovisioner.models.VendorModel;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.utils.CompanyIdentifiers;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

import static no.nordicsemi.android.meshprovisioner.models.SigModelParser.GENERIC_ON_OFF_CLIENT;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.ViewHolder> {
    private final Context mContext;
    private final List<ProvisionedMeshNode> mNodes = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;
    private MeshNetwork mNetwork;

    public NodeAdapter(@NonNull final Context context,
                       final MeshNetwork network,
                       @NonNull final LiveData<List<ProvisionedMeshNode>> provisionedNodesLiveData) {
        this.mContext = context;
        this.mNetwork=network;
        provisionedNodesLiveData.observe((LifecycleOwner) context, nodes -> {
            if (nodes != null) {
                mNodes.clear();
                mNodes.addAll(nodes);
                notifyDataSetChanged();
            }
        });
    }

    public void setOnItemClickListener(@NonNull final OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View layoutView = LayoutInflater.from(mContext).inflate(R.layout.network_item, parent, false);
        return new NodeAdapter.ViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final ProvisionedMeshNode node = mNodes.get(position);
        if (node != null) {
            // >>>, ADD, PANDA, Icons
            //final Integer cid = node.getCompanyIdentifier();
            //if ( null != cid && 0x0504 == cid )
            /* set item icon */
            for ( Element elem : node.getElements().values() ) {
                boolean isIconSet = false;
                for ( MeshModel modl : elem.getMeshModels().values() ) {
                    if ( modl instanceof GenericOnOffServerModel ) {
                        holder.icon.setImageResource(R.drawable.ic_lightbulb_outline_nordic_white_48dp);
                        isIconSet = true;
                    } else
                    if ( GENERIC_ON_OFF_CLIENT == modl.getModelId() ) {
                        holder.icon.setImageResource(R.drawable.ic_light_switch_nordic_medium_white_48dp);
                        isIconSet = true;
                    } else
                    if ( modl instanceof SensorServer ) {
                        holder.icon.setImageResource(R.drawable.ic_sensor_line_whit_48dp);
                        isIconSet = true;
                    }
                }
                if (isIconSet) break;
            }

            holder.nodebulb.setVisibility(View.GONE);
            holder.nodeHsls.setVisibility(View.GONE);
            holder.nodeVndr.setVisibility(View.GONE);
            holder.nodeSnsr.setVisibility(View.GONE);
            for ( Element elem : node.getElements().values() ) {
                for ( MeshModel modl : elem.getMeshModels().values() ) {
                    if ( modl instanceof GenericOnOffServerModel ) {
                        holder.nodebulb.setVisibility(View.VISIBLE);
                    }
                    if ( modl instanceof LightHslServer ) {
                        holder.nodeHsls.setVisibility(View.VISIBLE);
                    }
                    if ( modl instanceof SensorServer) {
                        holder.nodeSnsr.setVisibility(View.VISIBLE);
                    }
                    if ( modl instanceof VendorModel) {
                        holder.nodeVndr.setVisibility(View.VISIBLE);
                    }
                }
            }
            // <<<,
            holder.name.setText(node.getNodeName());
            holder.unicastAddress.setText(MeshParserUtils.bytesToHex(MeshAddress.addressIntToBytes(node.getUnicastAddress()), false));
            final Map<Integer, Element> elements = node.getElements();
            if (!elements.isEmpty()) {
                holder.nodeInfoContainer.setVisibility(View.VISIBLE);
//                if (node.getCompanyIdentifier() != null) {
//                    holder.companyIdentifier.setText(CompanyIdentifiers.getCompanyName(node.getCompanyIdentifier().shortValue()));
//                } else {
//                    holder.companyIdentifier.setText(R.string.unknown);
//                }
//                holder.elements.setText(String.valueOf(elements.size()));
//                holder.models.setText(String.valueOf(getModels(elements)));

                // >>>, ADD, PANDA, show group address, or groups number
                Set<Integer> grps = new HashSet<>();
                Integer      addr = 0;
                String str="没有配置";

                for (Element element : elements.values()) {
                    for ( MeshModel modl : element.getMeshModels().values() ) {
                        final List<Integer> list = modl.getSubscribedAddresses();
                        if (0 <  list.size()) {
                            grps.addAll(modl.getSubscribedAddresses());
                            addr = modl.getSubscribedAddresses().get(0);
                            str=mNetwork.getGroup(addr).getName();
                            for(Integer i=1;i<list.size();i++) {
                                addr = modl.getSubscribedAddresses().get(i);
                                str = str + "/" + mNetwork.getGroup(addr).getName();
                            }
                        }
                        holder.groups.setText(str);
                    }
                }
                if (1 == grps.size()) {
//                    holder.groups.setText(MeshParserUtils.bytesToHex(MeshParserUtils.intToBytes(addr), false))
                    String str1 = mNetwork.getGroup(addr).getName();
                    holder.groups.setText(str1);
                }
                else
                    holder.groups.setText(String.valueOf(grps.size()));
            } else {
//                holder.companyIdentifier.setText(R.string.unknown);
//                holder.elements.setText(String.valueOf(node.getNumberOfElements()));
//                holder.models.setText(R.string.unknown);
            }

            //holder.getSwipeableView().setTag(node);
        }
        holder.nodeVndr.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mNodes.size();
    }

    public ProvisionedMeshNode getItem(final int position) {
        if (mNodes.size() > 0 && position > -1) {
            return mNodes.get(position);
        }
        return null;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    private int getModels(final Map<Integer, Element> elements) {
        int models = 0;
        for (Element element : elements.values()) {
            models += element.getMeshModels().size();
        }
        return models;
    }

//    @FunctionalInterface
    public interface OnItemClickListener {
        void onCfgsClicked(final ProvisionedMeshNode node);
        void onUserClicked(final View view, final ProvisionedMeshNode node);
    }

    final class ViewHolder extends RemovableViewHolder {
        @BindView(R.id.container)
        FrameLayout container;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.node_name)
        TextView name;
        @BindView(R.id.configured_node_info_container)
        View nodeInfoContainer;
        @BindView(R.id.unicast)
        TextView unicastAddress;
//        @BindView(R.id.company_identifier)
//        TextView companyIdentifier;
//        @BindView(R.id.elements)
//        TextView elements;
//        @BindView(R.id.models)
//        TextView models;

        // >>>>, ADD, PANDA
        @BindView(R.id.groups)
        TextView groups;

        @BindView(R.id.ib_node_cfgs)
        ImageButton nodeCfgs;
        @BindView(R.id.ib_node_rset)
        ImageButton nodeRset;

        @BindView(R.id.ib_node_bulb)
        ImageButton nodebulb;
        @BindView(R.id.ib_node_hsls)
        ImageButton nodeHsls;
        @BindView(R.id.ib_node_snsr)
        ImageButton nodeSnsr;
        @BindView(R.id.ib_node_vndr)
        ImageButton nodeVndr;
        // <<<<,

        private ViewHolder(final View provisionedView) {
            super(provisionedView);
            ButterKnife.bind(this, provisionedView);

            nodebulb.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });

            nodeHsls.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });

            nodeSnsr.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });

            nodeVndr.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });

            container.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onCfgsClicked(mNodes.get(getAdapterPosition()));
                }
            });

            nodeCfgs.setOnClickListener(view -> {
                if ( null != mOnItemClickListener ) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });

            nodeRset.setOnClickListener(view -> {
                if ( null != mOnItemClickListener ) {
                    mOnItemClickListener.onUserClicked(view, mNodes.get(getAdapterPosition()));
                }
            });
        }
    }
}
