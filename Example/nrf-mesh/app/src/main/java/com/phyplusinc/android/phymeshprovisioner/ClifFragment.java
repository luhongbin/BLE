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

package com.phyplusinc.android.phymeshprovisioner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.phyplusinc.android.phymeshprovisioner.adapter.GroupAdapter;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentCreateGroup;
import com.phyplusinc.android.phymeshprovisioner.node.dialog.BottomSheetVendorDialogFragment;
import com.phyplusinc.android.phymeshprovisioner.utils.HexKeyListener;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;
import com.phyplusinc.android.phymeshprovisioner.widgets.ItemTouchHelperAdapter;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableItemTouchHelperCallback;
import com.phyplusinc.android.phymeshprovisioner.widgets.RemovableViewHolder;

import java.util.UUID;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class ClifFragment extends Fragment /*implements Injectable,
        ItemTouchHelperAdapter,
        GroupAdapter.OnItemClickListener,
        GroupCallbacks*/ {
    private static final String AKEY_INDX = "AKEY_INDX";
    private static final String MODL_NUMB = "MODL_NUMB";

    private int mIntModlNumb;
    private int mIntAKeyIndx;

    private View mVwMsgsCtnr;
    private TextView mTvRcvsMsgs;


    public static ClifFragment getInstance(final int modelId, final int appKeyIndex) {
        final ClifFragment fragment = new ClifFragment();
        final Bundle args = new Bundle();
        args.putInt(MODL_NUMB, modelId);
        args.putInt(AKEY_INDX, appKeyIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mIntModlNumb = getArguments().getInt(MODL_NUMB);
            mIntAKeyIndx = getArguments().getInt(AKEY_INDX);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View vwRoot = inflater.inflate(R.layout.fragment_clif, null);


        final CheckBox cbAcks = vwRoot.findViewById(R.id.chk_acknowledged);
        final TextInputLayout ilOpCo = vwRoot.findViewById(R.id.op_code_layout);
        final TextInputEditText etOpCo = vwRoot.findViewById(R.id.op_code);

        final KeyListener klHkey = new HexKeyListener();

        final TextInputLayout ilPara = vwRoot.findViewById(R.id.parameters_layout);
        final TextInputEditText etPara = vwRoot.findViewById(R.id.parameters);
        mVwMsgsCtnr = vwRoot.findViewById(R.id.received_message_container);
        mTvRcvsMsgs = vwRoot.findViewById(R.id.received_message);
        final Button btSend = vwRoot.findViewById(R.id.action_send);

        etOpCo.setKeyListener(klHkey);
        etOpCo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                ilPara.setError(null);
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        etPara.setKeyListener(klHkey);
        etPara.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                ilPara.setError(null);
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });


        btSend.setOnClickListener(v -> {
                    mVwMsgsCtnr.setVisibility(View.GONE);
                    mTvRcvsMsgs.setText("");
            final String strOpCo = etOpCo.getEditableText().toString().trim();
            final String strPara = etPara.getEditableText().toString().trim();

            if (!validateOpcode(strOpCo, ilOpCo))
                return;

            if (!validateParameters(strPara, ilPara))
                return;

            final byte[] params;
            if (TextUtils.isEmpty(strPara) && strPara.length() == 0) {
                params = null;
            } else {
                params = MeshParserUtils.toByteArray(strPara);
            }

            ((BottomSheetVendorDialogFragment.BottomSheetVendorModelControlsListener) requireActivity())
                    .sendVendorModelMessage(mIntModlNumb, mIntAKeyIndx, Integer.parseInt(strOpCo, 16), params, cbAcks.isChecked());

////            coun = 50;
////            hdlr.postDelayed(new Runnable() {
////                @Override
////                public void run() {
////                    params[2] = (byte) (coun % 2);
////                    ((BottomSheetVendorModelControlsListener) requireActivity())
////                            .sendVendorModelMessage(mModelId, mKeyIndex, Integer.parseInt(opCode, 16), params, chkAcknowledged.isChecked());
////                    coun--;
////                if (0 < coun)
////                hdlr.postDelayed(this, 100);
////                }
////            }, 10000);
        });

        return vwRoot;

    }

    /**
     * Validate opcode
     *
     * @param opCode       opcode
     * @param opCodeLayout op c0de view
     * @return true if success or false otherwise
     */
    private boolean validateOpcode(final String opCode, final TextInputLayout opCodeLayout) {
        try {
            if (TextUtils.isEmpty(opCode)) {
                opCodeLayout.setError(getString(R.string.error_empty_value));
                return false;
            }

            if (opCode.length() % 2 != 0 || !opCode.matches(Utils.HEX_PATTERN)) {
                opCodeLayout.setError(getString(R.string.invalid_hex_value));
                return false;
            }
            if (MeshParserUtils.isValidOpcode(Integer.valueOf(opCode, 16))) {
                return true;
            }
        } catch (NumberFormatException ex) {
            opCodeLayout.setError(getString(R.string.invalid_value));
            return false;
        } catch (IllegalArgumentException ex) {
            opCodeLayout.setError(ex.getMessage());
            return false;
        } catch (Exception ex) {
            opCodeLayout.setError(ex.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Validate parameters
     *
     * @param parameters       parameters
     * @param parametersLayout parameter view
     * @return true if success or false otherwise
     */
    private boolean validateParameters(final String parameters, final TextInputLayout parametersLayout) {
        try {
            if (TextUtils.isEmpty(parameters) && parameters.length() == 0) {
                return true;
            }

            if (parameters.length() % 2 != 0 || !parameters.matches(Utils.HEX_PATTERN)) {
                parametersLayout.setError(getString(R.string.invalid_hex_value));
                return false;
            }

            if (MeshParserUtils.isValidParameters(MeshParserUtils.toByteArray(parameters))) {
                return true;
            }
        } catch (NumberFormatException ex) {
            parametersLayout.setError(getString(R.string.invalid_value));
            return false;
        } catch (IllegalArgumentException ex) {
            parametersLayout.setError(ex.getMessage());
            return false;
        } catch (Exception ex) {
            parametersLayout.setError(ex.getMessage());
            return false;
        }
        return true;
    }

//    public void setReceivedMessage(final byte[] accessPayload) {
//        messageContainer.setVisibility(View.VISIBLE);
//        receivedMessage.setText(MeshParserUtils.bytesToHex(accessPayload, false));
//    }

//    @Override
//    public void onItemClick(final int address) {
//        mViewModel.setSelectedGroup(address);
//        startActivity(new Intent(requireContext(), GroupControlsActivity.class));
//    }
//
//    @Override
//    public void onItemDismiss(final RemovableViewHolder viewHolder) {
//        final int position = viewHolder.getAdapterPosition();
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        final Group group = network.getGroups().get(position);
//        if (network.getModels(group).size() == 0) {
//            network.removeGroup(group);
//            displaySnackBar(group);
//        }
//    }
//
//    @Override
//    public void onItemDismissFailed(final RemovableViewHolder viewHolder) {
//        final String message = getString(R.string.error_group_unsubscribe_to_delete);
//        mViewModel.displaySnackBar(requireActivity(), container, message, Snackbar.LENGTH_LONG);
//    }
//
//    @Override
//    public Group createGroup() {
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        return network.createGroup(network.getSelectedProvisioner(), "Mesh Group");
//    }
//
//    @Override
//    public Group createGroup(@NonNull final String name) {
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        return network.createGroup(network.getSelectedProvisioner(), name);
//    }
//
//    @Override
//    public Group createGroup(@NonNull final UUID uuid, final String name) {
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        return network.createGroup(uuid, null, name);
//    }
//
//    @Override
//    public boolean onGroupAdded(@NonNull final String name, final int address) {
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        final Group group = network.createGroup(network.getSelectedProvisioner(), address, name);
//        if (group != null) {
//            return network.addGroup(group);
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onGroupAdded(@NonNull final Group group) {
//        final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//        return network.addGroup(group);
//    }

//    private void displaySnackBar(final Group group) {
//        final String message = getString(R.string.group_deleted, group.getName());
//        Snackbar.make(container, message, Snackbar.LENGTH_LONG)
//                .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
//                .setAction(R.string.undo, v -> {
//                    mEmptyView.setVisibility(View.INVISIBLE);
//                    final MeshNetwork network = mViewModel.getNetworkLiveData().getMeshNetwork();
//                    if (network != null) {
//                        network.addGroup(group);
//                    }
//
//                })
//                .show();
//    }
}
