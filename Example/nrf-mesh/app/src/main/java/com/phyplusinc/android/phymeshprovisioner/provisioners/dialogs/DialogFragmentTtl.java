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
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import com.phyplusinc.android.phymeshprovisioner.R;

public class DialogFragmentTtl extends DialogFragment {

    private static final String GLOBAL_TTL = "GLOBAL_TTL";

    //UI Bindings
    @BindView(R.id.text_input_layout)
    TextInputLayout ttlInputLayout;
    @BindView(R.id.text_input)
    TextInputEditText ttlInput;

    private int mTtl;

    public static DialogFragmentTtl newInstance(final int ttl) {
        DialogFragmentTtl fragmentNetworkKey = new DialogFragmentTtl();
        final Bundle args = new Bundle();
        args.putInt(GLOBAL_TTL, ttl);
        fragmentNetworkKey.setArguments(args);
        return fragmentNetworkKey;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTtl = getArguments().getInt(GLOBAL_TTL);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fragment_ttl, null);

        //Bind ui
        ButterKnife.bind(this, rootView);
        final String ttl = String.valueOf(mTtl);
        ttlInput.setText(ttl);
        ttlInput.setSelection(ttl.length());
        ttlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    ttlInputLayout.setError(getString(R.string.error_empty_ttl));
                } else {
                    ttlInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext()).setView(rootView)
                .setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null);

        alertDialogBuilder.setIcon(R.drawable.ic_timer);
        alertDialogBuilder.setTitle(R.string.title_ttl);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String globalTTL = ttlInput.getEditableText().toString().trim();
            try {
                if (validateInput(globalTTL)) {
                    if (((DialogFragmentTtlListener) requireActivity()).setDefaultTtl(Integer.parseInt(globalTTL))) {
                        dismiss();
                    }
                }
            } catch (IllegalArgumentException ex) {
                ttlInputLayout.setError(ex.getMessage());
            }
        });

        return alertDialog;
    }

    private boolean validateInput(@NonNull final String input) {
        if (TextUtils.isEmpty(input)) {
            ttlInputLayout.setError("TTL value cannot be empty");
            return false;
        }

        if (!MeshParserUtils.isValidDefaultTtl(Integer.parseInt(input))) {
            throw new IllegalArgumentException(getString(R.string.error_invalid_default_ttl));
        }
        return true;
    }

    public interface DialogFragmentTtlListener {

        boolean setDefaultTtl(final int ttl);

    }
}
