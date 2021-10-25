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

package com.phyplusinc.android.phymeshprovisioner.node.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import com.phyplusinc.android.phymeshprovisioner.R;

public class DialogFragmentElementName extends DialogFragment {

    private static final String ELEMENT = "ELEMENT";

    //UI Bindings
    @BindView(R.id.text_input_layout)
    TextInputLayout elementNameInputLayout;
    @BindView(R.id.text_input)
    TextInputEditText elementNameInput;

    private Element mElement;

    public static DialogFragmentElementName newInstance(@NonNull final Element element) {
        DialogFragmentElementName fragmentNetworkKey = new DialogFragmentElementName();
        final Bundle args = new Bundle();
        args.putParcelable(ELEMENT, element);
        fragmentNetworkKey.setArguments(args);
        return fragmentNetworkKey;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mElement = getArguments().getParcelable(ELEMENT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fragment_name, null);

        //Bind ui
        ButterKnife.bind(this, rootView);
        final TextView summary = rootView.findViewById(R.id.summary);
        elementNameInputLayout.setHint(getString(R.string.hint_friendly_name));
        elementNameInput.setText(mElement.getName());
        elementNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString().trim())) {
                    elementNameInputLayout.setError(getString(R.string.error_empty_name));
                } else {
                    elementNameInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext()).setView(rootView)
                .setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null);

        alertDialogBuilder.setIcon(R.drawable.ic_label_black_alpha_24dp);
        alertDialogBuilder.setTitle(R.string.title_element_name);
        summary.setText(R.string.element_name_rationale);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String elementName = elementNameInput.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(elementName)) {
                if (((DialogFragmentElementNameListener) requireContext()).onElementNameUpdated(mElement, elementName)) {
                    dismiss();
                }
            }
        });

        return alertDialog;
    }

    public interface DialogFragmentElementNameListener {

        boolean onElementNameUpdated(@NonNull final Element element, @NonNull final String name);

    }
}
