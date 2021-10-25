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

package com.phyplusinc.android.phymeshprovisioner.provisioners.dialogs;

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
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.w3c.dom.Text;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.AllocatedGroupRange;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.provisioners.RangeListener;
import com.phyplusinc.android.phymeshprovisioner.utils.HexKeyListener;

public class DialogFragmentGroupRange extends DialogFragment {

    private static final String RANGE = "RANGE";
    //UI Bindings
    @BindView(R.id.low_address_layout)
    TextInputLayout lowAddressInputLayout;
    @BindView(R.id.low_address_input)
    TextInputEditText lowAddressInput;
    @BindView(R.id.high_address_layout)
    TextInputLayout highAddressInputLayout;
    @BindView(R.id.high_address_input)
    TextInputEditText highAddressInput;

    private AllocatedGroupRange mRange;

    public static DialogFragmentGroupRange newInstance(@Nullable final AllocatedGroupRange range) {
        DialogFragmentGroupRange fragment = new DialogFragmentGroupRange();
        final Bundle args = new Bundle();
        args.putParcelable(RANGE, range);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRange = getArguments().getParcelable(RANGE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fragment_range, null);

        //Bind ui
        ButterKnife.bind(this, rootView);
        final TextView summary = rootView.findViewById(R.id.summary);
        if (mRange != null) {
            final String lowAddress = MeshAddress.formatAddress(mRange.getLowAddress(), false);
            final String highAddress = MeshAddress.formatAddress(mRange.getHighAddress(), false);
            lowAddressInput.setText(lowAddress);
            lowAddressInput.setSelection(lowAddress.length());
            highAddressInput.setText(highAddress);
            highAddressInput.setSelection(highAddress.length());
        }

        final KeyListener hexKeyListener = new HexKeyListener();
        lowAddressInput.setKeyListener(hexKeyListener);
        lowAddressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    lowAddressInputLayout.setError(getString(R.string.error_empty_value));
                } else {
                    lowAddressInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        highAddressInput.setKeyListener(hexKeyListener);
        highAddressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    highAddressInputLayout.setError(getString(R.string.error_empty_value));
                } else {
                    highAddressInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext())
                .setView(rootView)
                .setIcon(R.drawable.ic_arrow_collapse_black_alpha_24dp)
                .setTitle(R.string.title_range)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        summary.setText(R.string.group_range_summary);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String low = lowAddressInput.getEditableText().toString().trim();
            final String high = highAddressInput.getEditableText().toString().trim();
            if (validateLow(low) && validateHigh(high)) {
                try {
                    AllocatedGroupRange range = mRange;
                    if (range == null) {
                        range = new AllocatedGroupRange(Integer.parseInt(low, 16), Integer.parseInt(high, 16));
                    } else {
                        range.setLowAddress(Integer.parseInt(low, 16));
                        range.setHighAddress(Integer.parseInt(high, 16));
                    }
                    ((RangeListener) requireActivity()).addRange(range);
                    dismiss();
                } catch (IllegalArgumentException ex) {
                    lowAddressInputLayout.setError(ex.getMessage());
                }
            }
        });

        return alertDialog;
    }

    private boolean validateLow(@NonNull final String addressValue) {
        try {

            if(TextUtils.isEmpty(addressValue)){
                lowAddressInputLayout.setError(getString(R.string.error_empty_value));
                return false;
            }

            final int address = Integer.parseInt(addressValue, 16);
            if (!MeshAddress.isValidGroupAddress(address)) {
                lowAddressInputLayout.setError("Group address value must range from 0xC000 - 0xFEFF");
                return false;
            }
        } catch (IllegalArgumentException ex) {
            lowAddressInputLayout.setError(ex.getMessage());
            return false;
        }
        return true;
    }

    private boolean validateHigh(final String addressValue) {
        try {

            if(TextUtils.isEmpty(addressValue)){
                highAddressInputLayout.setError("High address value cannot be empty");
                return false;
            }

            final int address = Integer.parseInt(addressValue, 16);
            if (!MeshAddress.isValidGroupAddress(address)) {
                highAddressInputLayout.setError("Group address value must range from 0xC000 - 0xFEFF");
                return false;
            }
        } catch (IllegalArgumentException ex) {
            highAddressInputLayout.setError(ex.getMessage());
            return false;
        }
        return true;
    }
}
