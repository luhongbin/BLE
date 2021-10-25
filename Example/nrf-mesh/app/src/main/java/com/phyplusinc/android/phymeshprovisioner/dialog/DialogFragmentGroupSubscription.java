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

package com.phyplusinc.android.phymeshprovisioner.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import com.phyplusinc.android.phymeshprovisioner.GroupCallbacks;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.AddressTypeAdapter;
import com.phyplusinc.android.phymeshprovisioner.adapter.GroupAdapterSpinner;
import com.phyplusinc.android.phymeshprovisioner.utils.AddressTypes;
import com.phyplusinc.android.phymeshprovisioner.utils.HexKeyListener;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.phyplusinc.android.phymeshprovisioner.utils.AddressTypes.GROUP_ADDRESS;
import static com.phyplusinc.android.phymeshprovisioner.utils.AddressTypes.VIRTUAL_ADDRESS;

public class DialogFragmentGroupSubscription extends DialogFragment {

    private static final AddressTypes[] addressTypes = {GROUP_ADDRESS, VIRTUAL_ADDRESS};
    private static final String ARGS_GROUPS = "ARGS_GROUPS";
    private static final String GROUP = "GROUP";
    private static final String ARGS_ACTI = "ARGS_ACTI";

    public static final int    ACTI_SUBS = 0;
    public static final int    ACTI_BIND = 1;

    //UI Bindings
    @BindView(R.id.address_types)
    Spinner addressTypesSpinnerView;
    @BindView(R.id.group_container)
    View groupContainer;
    @BindView(R.id.radio_select_group)
    RadioButton selectGroup;
    @BindView(R.id.radio_create_group)
    RadioButton createGroup;
    @BindView(R.id.groups)
    Spinner groups;
    @BindView(R.id.group_name_layout)
    TextInputLayout groupNameInputLayout;
    @BindView(R.id.name_input)
    TextInputEditText groupNameInput;
    @BindView(R.id.group_address_layout)
    TextInputLayout addressInputLayout;
    @BindView(R.id.address_input)
    TextInputEditText addressInput;
    @BindView(R.id.no_groups_configured)
    TextView noGroups;
    @BindView(R.id.label_summary)
    TextView labelSummary;
    @BindView(R.id.uuid_label)
    TextView labelUuidView;

    private Button mGenerateLabelUUID;

    private AddressTypeAdapter mAdapterSpinner;

    private ArrayList<Group> mGroups;
    private Group mGroup;

    private int mArgsActi;

    public static DialogFragmentGroupSubscription newInstance(final ArrayList<Group> groups) {
        final DialogFragmentGroupSubscription fragment = new DialogFragmentGroupSubscription();
        final Bundle args = new Bundle();
        args.putParcelableArrayList(ARGS_GROUPS, groups);
        fragment.setArguments(args);
        return fragment;
    }

    public static DialogFragmentGroupSubscription newInstance(final ArrayList<Group> groups, final int acti) {
        final DialogFragmentGroupSubscription fragment = new DialogFragmentGroupSubscription();
        final Bundle args = new Bundle();
        args.putParcelableArrayList(ARGS_GROUPS, groups);
        args.putInt(ARGS_ACTI, acti);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mGroups = getArguments().getParcelableArrayList(ARGS_GROUPS);
            mArgsActi = getArguments().getInt(ARGS_ACTI, ACTI_SUBS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = LayoutInflater.from(getContext()).
                inflate(R.layout.dialog_fragment_group_subscription, null);

        //Bind ui
        ButterKnife.bind(this, rootView);
        if (savedInstanceState == null) {
            mGroup = ((GroupCallbacks) requireActivity()).createGroup();
        } else {
            mGroup = savedInstanceState.getParcelable(GROUP);
        }

        selectGroup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            groupNameInputLayout.setEnabled(!isChecked);
            groupNameInputLayout.setError(null);
            addressInputLayout.setEnabled(!isChecked);
            addressInputLayout.setError(null);
            groups.setEnabled(isChecked);
            createGroup.setChecked(!isChecked);
        });

        createGroup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            groupNameInputLayout.setEnabled(isChecked);
            groupNameInputLayout.setError(null);
            addressInputLayout.setEnabled(isChecked);
            addressInputLayout.setError(null);
            groups.setEnabled(!isChecked);
            selectGroup.setChecked(!isChecked);
            if (isChecked) {
                if (mGroup != null) {
                    groupNameInput.setText(mGroup.getName());
                    addressInput.setText(MeshAddress.formatAddress(mGroup.getAddress(), false));
                }
            }
        });

        mAdapterSpinner = new AddressTypeAdapter(requireContext(), addressTypes);
        addressTypesSpinnerView.setAdapter(mAdapterSpinner);

        final GroupAdapterSpinner adapter = new GroupAdapterSpinner(requireContext(), mGroups);
        groups.setAdapter(adapter);

        if (mGroups.isEmpty()) {
            selectGroup.setEnabled(false);
            groups.setEnabled(false);
            createGroup.setChecked(true);
        } else {
            selectGroup.setChecked(true);
            createGroup.setChecked(false);
        }

        updateGroup();

        addressTypesSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                updateAddress(mAdapterSpinner.getItem(position));
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {

            }
        });

        final KeyListener hexKeyListener = new HexKeyListener();
        addressInput.setKeyListener(hexKeyListener);
        addressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                mGroup = null;
                if (TextUtils.isEmpty(s.toString())) {
                    addressInputLayout.setError(getString(R.string.error_empty_group_address));
                } else {
                    addressInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.ic_subscribe_black_alpha_24dp)
                .setTitle(mArgsActi == ACTI_SUBS ? R.string.title_subscribe_group : R.string.title_subsc_group_address)
                .setView(rootView)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.generate_uuid, null);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final AddressTypes type = (AddressTypes) addressTypesSpinnerView.getSelectedItem();
            try {
                if (type == GROUP_ADDRESS) {
                    if (createGroup.isChecked()) {
                        if (mGroup != null) {
                            if (((GroupCallbacks) requireActivity()).onGroupAdded(mGroup)) {
                                dismiss();
                            }
                        } else {
                            final String name = groupNameInput.getEditableText().toString().trim();
                            final String address = addressInput.getEditableText().toString().trim();
                            if (validateInput(name, address)) {
                                if ((((GroupCallbacks) requireActivity())).
                                        onGroupAdded(name, Integer.valueOf(address, 16))) {
                                    dismiss();
                                }
                            }
                        }
                    } else {
                        final Group group = (Group) groups.getSelectedItem();
                        ((GroupCallbacks) requireActivity()).subscribe(group, mArgsActi);
                        dismiss();
                    }
                } else {
                    final UUID uuid = UUID.fromString(labelUuidView.getText().toString().trim());
                    final String name = groupNameInput.getEditableText().toString().trim();
                    final Group group = ((GroupCallbacks) requireActivity()).createGroup(uuid, name);
                    if (group != null) {
                        if (((GroupCallbacks) requireActivity()).onGroupAdded(group)) {
                            dismiss();
                        }
                    }
                }
            } catch (IllegalArgumentException ex) {
                addressInputLayout.setError(ex.getMessage());
            }
        });

        mGenerateLabelUUID = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        mGenerateLabelUUID.setOnClickListener(v -> {
            final UUID uuid = MeshAddress.generateRandomLabelUUID();
            labelUuidView.setText(uuid.toString().toUpperCase(Locale.US));
            generateVirtualAddress(uuid);
        });

        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(GROUP, mGroup);
    }

    private void updateAddress(@NonNull final AddressTypes addressType) {
        if (addressType == VIRTUAL_ADDRESS) {
            labelSummary.setVisibility(VISIBLE);
            labelUuidView.setVisibility(VISIBLE);
            mGenerateLabelUUID.setVisibility(VISIBLE);
            groupContainer.setVisibility(GONE);
            groupNameInputLayout.setEnabled(true);
            groupNameInputLayout.setError(null);
            addressInputLayout.setError(null);
            addressInputLayout.setEnabled(false);
            generateVirtualAddress(UUID.fromString(labelUuidView.getText().toString()));
        } else {
            groupContainer.setVisibility(VISIBLE);
            labelSummary.setVisibility(GONE);
            labelUuidView.setVisibility(GONE);
            mGenerateLabelUUID.setVisibility(GONE);
            updateGroup();
        }
    }

    private void generateVirtualAddress(@NonNull final UUID uuid) {
        final Integer add = MeshAddress.generateVirtualAddress(uuid);
        addressInput.setText(MeshAddress.formatAddress(add, false));
    }

    private void updateGroup() {
        if (mGroup == null) {
            mGroup = ((GroupCallbacks) requireActivity()).createGroup();
        }

        if (mGroup != null) {
            groupNameInput.setText(mGroup.getName());
            addressInput.setText(MeshAddress.formatAddress(mGroup.getAddress(), false));
        }
    }

    private boolean validateInput(@NonNull final String name, @NonNull final String address) {
        try {
            if (TextUtils.isEmpty(name)) {
                groupNameInputLayout.setError(getString(R.string.error_empty_group_name));
                return false;
            }
            if (address.length() % 4 != 0 || !address.matches(Utils.HEX_PATTERN)) {
                addressInputLayout.setError(getString(R.string.invalid_address_value));
                return false;
            }

            final int groupAddress = Integer.valueOf(address, 16);
            if (!MeshAddress.isValidGroupAddress(groupAddress)) {
                addressInputLayout.setError(getString(R.string.invalid_address_value));
                return false;
            }

            for (Group group : mGroups) {
                if (groupAddress == group.getAddress()) {
                    addressInputLayout.setError(getString(R.string.error_group_address_in_used));
                    return false;
                }
            }
        } catch (IllegalArgumentException ex) {
            addressInputLayout.setError(ex.getMessage());
            return false;
        }

        return true;
    }
}
