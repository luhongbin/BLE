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
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.provisionerstates.ProvisioningConfirmationState;
import no.nordicsemi.android.meshprovisioner.provisionerstates.UnprovisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.utils.AuthenticationOOBMethods;
import no.nordicsemi.android.meshprovisioner.utils.InputOOBAction;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.OutputOOBAction;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.utils.HexKeyListener;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;

import static android.graphics.Typeface.BOLD;

public class DialogFragmentAuthenticationInput extends DialogFragment {

    //UI Bindings
    @BindView(R.id.summary)
    TextView dialogSummary;
    @BindView(R.id.text_input_layout)
    TextInputLayout pinInputLayout;
    @BindView(R.id.hex_prefix)
    TextView hexPrefix;
    @BindView(R.id.text_input)
    TextInputEditText pinInput;

    private UnprovisionedMeshNode mNode;

    public interface ProvisionerInputFragmentListener {
        void onPinInputComplete(final String pin);

        void onPinInputCanceled();
    }

    public static DialogFragmentAuthenticationInput newInstance(final UnprovisionedMeshNode node) {
        Bundle args = new Bundle();
        DialogFragmentAuthenticationInput fragment = new DialogFragmentAuthenticationInput();
        args.putParcelable(Utils.EXTRA_DATA, node);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNode = getArguments().getParcelable(Utils.EXTRA_DATA);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        final View rootView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_fragment_auth_input, null);
        ButterKnife.bind(this, rootView);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireContext()).
                setIcon(R.drawable.ic_lock_open_black_alpha_24dp).
                setTitle(getString(R.string.provisioner_authentication_title)).
                setView(rootView);

        updateAuthUI(alertDialogBuilder);
        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);

        final Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(button != null) {
            button.setOnClickListener(v -> {
                final String pin = pinInput.getEditableText().toString().trim();
                if (validateInput(pin)) {
                    ((ProvisionerInputFragmentListener) requireActivity()).onPinInputComplete(pin);
                    dismiss();
                }
            });
        }

        return alertDialog;
    }

    private boolean validateInput(final String input) {
        if (TextUtils.isEmpty(input)) {
            pinInputLayout.setError(getString(R.string.error_empty_pin));
            return false;
        }

        if(mNode.getAuthMethodUsed() == AuthenticationOOBMethods.STATIC_OOB_AUTHENTICATION){
            if(input.length() != 32) {
                pinInputLayout.setError(getString(R.string.error_invalid_static_oob));
                return false;
            }

            final byte[] staticOObKey = MeshParserUtils.toByteArray(input);
            if (staticOObKey.length != 16) {
                pinInputLayout.setError(getString(R.string.error_invalid_static_oob));
                return false;
            }
        }

        return true;
    }

    private void updateAuthUI(final AlertDialog.Builder alertDialogBuilder) {
        switch (mNode.getAuthMethodUsed()) {
            case STATIC_OOB_AUTHENTICATION:
                updateStaticOOBUI();
                alertDialogBuilder.
                        setPositiveButton(getString(R.string.confirm), null).
                        setNegativeButton(getString(R.string.cancel),
                                (dialog, which) -> ((ProvisionerInputFragmentListener) requireActivity()).onPinInputCanceled());
                break;
            case OUTPUT_OOB_AUTHENTICATION:
                updateOutputOOBUI();
                alertDialogBuilder.
                        setPositiveButton(getString(R.string.confirm), null).
                        setNegativeButton(getString(R.string.cancel),
                                (dialog, which) -> ((ProvisionerInputFragmentListener) requireActivity()).onPinInputCanceled());
                break;
            case INPUT_OOB_AUTHENTICATION:
                updateInputOOBUI();
                alertDialogBuilder.
                        setNegativeButton(getString(R.string.cancel),
                                (dialog, which) -> ((ProvisionerInputFragmentListener) requireActivity()).onPinInputCanceled());
                break;
        }

        pinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    pinInputLayout.setError(getString(R.string.error_empty_pin));
                } else {
                    pinInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });
    }

    private void updateStaticOOBUI() {
        dialogSummary.setText(R.string.provisioner_input_static_oob);
        hexPrefix.setVisibility(View.VISIBLE);
        pinInput.setInputType(InputType.TYPE_CLASS_TEXT);
        pinInput.setHint(getString((R.string.hint_static_oob)));
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ProvisioningConfirmationState.AUTH_VALUE_LENGTH * 2)});
        pinInput.setKeyListener(new HexKeyListener());
    }

    private void updateOutputOOBUI() {
        final OutputOOBAction outputOOBAction = OutputOOBAction.fromValue(mNode.getAuthActionUsed());
        if (outputOOBAction == OutputOOBAction.BLINK ||
                outputOOBAction == OutputOOBAction.BEEP ||
                outputOOBAction == OutputOOBAction.VIBRATE) {
            pinInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            pinInput.setHint(getString((R.string.hint_numeric_action)));
            pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mNode.getProvisioningCapabilities().getOutputOOBSize())});
            if (outputOOBAction == OutputOOBAction.BLINK) {
                dialogSummary.setText(R.string.provisioner_input_blinks);
            } else if (outputOOBAction == OutputOOBAction.BEEP) {
                dialogSummary.setText(R.string.provisioner_input_beeps);
            } else {
                dialogSummary.setText(R.string.provisioner_input_vibrations);
            }

        } else if (outputOOBAction == OutputOOBAction.OUTPUT_NUMERIC) {
            pinInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            pinInput.setHint(getString((R.string.hint_numeric)));
            pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mNode.getProvisioningCapabilities().getOutputOOBSize())});
            dialogSummary.setText(R.string.provisioner_input_numeric);
        } else {
            pinInput.setInputType(InputType.TYPE_CLASS_TEXT);
            pinInput.setHint(getString((R.string.hint_alpha_numeric)));
            pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mNode.getProvisioningCapabilities().getOutputOOBSize())});
            dialogSummary.setText(R.string.provisioner_input_numeric);
        }
        hexPrefix.setVisibility(View.GONE);
    }

    private void updateInputOOBUI() {
        final InputOOBAction inputOOBAction = InputOOBAction.fromValue(mNode.getAuthActionUsed());
        pinInputLayout.setVisibility(View.GONE);
        final String msg;
        final SpannableStringBuilder spannableMessage;
        final int start;
        final int end;
        final byte[] authValue = mNode.getInputAuthentication();
        if (inputOOBAction == InputOOBAction.PUSH || inputOOBAction == InputOOBAction.TWIST) {
            //noinspection ConstantConditions
            final int authInput = MeshParserUtils.unsignedByteToInt(authValue[0]);
            if (inputOOBAction == InputOOBAction.PUSH) {
                msg = getResources().getQuantityString(R.plurals.input_pushes, authInput, authInput);
            } else {
                msg = getString(authInput);
            }
            spannableMessage = new SpannableStringBuilder(msg);
            start = msg.indexOf(String.valueOf(authInput));
            end = start + String.valueOf(authInput).length();
        } else if (inputOOBAction == InputOOBAction.INPUT_NUMERIC) {
            final String authString = String.valueOf(ByteBuffer.wrap(authValue).getInt());
            msg = getString(R.string.provisioner_input_numeric_device, authString);
            start = msg.indexOf(authString);
            end = start + authString.length();
            spannableMessage = new SpannableStringBuilder(msg);
        } else {
            final String authString = new String(authValue);
            msg = getString(R.string.provisioner_input_numeric_device, authString);
            start = msg.indexOf(authString);
            end = start + authString.length();
            spannableMessage = new SpannableStringBuilder(msg);
        }
        spannableMessage.setSpan(new StyleSpan(BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        dialogSummary.setText(spannableMessage);
        hexPrefix.setVisibility(View.GONE);
    }
}
