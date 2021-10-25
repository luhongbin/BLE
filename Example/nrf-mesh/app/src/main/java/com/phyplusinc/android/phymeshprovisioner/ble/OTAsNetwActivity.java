package com.phyplusinc.android.phymeshprovisioner.ble;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.phyplusinc.android.otas.PhyOTAsUtils;
import com.phyplusinc.android.phymeshprovisioner.R;
import com.phyplusinc.android.phymeshprovisioner.adapter.ExtendedBluetoothDevice;
import com.phyplusinc.android.phymeshprovisioner.dialog.DialogFragmentOtasComplete;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.ppsp.BtLe.PhyPPSPMngr;
import com.phyplusinc.android.ppsp.trns.PhyMsgsHdlr;


import java.nio.ByteBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OTAsNetwActivity extends AppCompatActivity implements
        PhyPPSPMngr.PPSPMngrCallbacks,
        DialogFragmentOtasComplete.OtasCompleteListener {
    private static final String TAG = OTAsNetwActivity.class.getSimpleName();

    private static final String DIALOG_FRAGMENT_OTAS_EXCEPTION = "DIALOG_FRAGMENT_OTAS_EXCEPTION";
    private static final String DIALOG_FRAGMENT_OTAS_COMPLETED = "DIALOG_FRAGMENT_OTAS_COMPLETED";

    private static final int REQUEST_STORAGE_PERMISSION = 2023; // random number
    private static final int READ_FILE_REQUEST_CODE = 42;
    private static final int SLCT_NODE_REQUEST_CODE = 43;

//    @Inject
//    ViewModelProvider.Factory mViewModelFactory;

    //UI Bindings
    @BindView(R.id.pb_prog)
    ProgressBar mProgressbar;
    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(R.id.scroll_container)
    ScrollView scrollView;
    @BindView(R.id.cv_node)
    CardView mCardNode;

    //    private NetKeysViewModel mViewModel;
//    private ManageNetKeyAdapter mAdapter;
    private boolean mIsActivityResumed = false;
    private String mAddr;

    private PhyOTAsUtils otasdkUtils = null;
    private static PhyPPSPMngr mOtasMngr = null;

    PhyMsgsHdlr mMsgsHdlr = new PhyMsgsHdlr();
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otas_netw);

        //Bind ui
        ButterKnife.bind(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.title_update_firmware);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        final View containerName = findViewById(R.id.container_file);
//        containerName.findViewById(R.id.image).
//                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
//        final TextView nameTitl = containerName.findViewById(R.id.title);
//        nameTitl.setText(R.string.firmware_file);
//        final TextView nameView = containerName.findViewById(R.id.text);
//        nameView.setVisibility(View.VISIBLE);
//
//        final View containerType = findViewById(R.id.container_type);
//        containerType.findViewById(R.id.image).
//                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
//        final TextView typeTitl = containerType.findViewById(R.id.title);
//        typeTitl.setText(R.string.firmware_type);
//        final TextView typeView = containerType.findViewById(R.id.text);
//        typeView.setVisibility(View.VISIBLE);
//
//        final View containerSize = findViewById(R.id.container_size);
//        containerSize.findViewById(R.id.image).
//                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
//        final TextView sizeTitl = containerSize.findViewById(R.id.title);
//        sizeTitl.setText(R.string.firmware_size);
//        final TextView sizeView = containerSize.findViewById(R.id.text);
//        sizeView.setVisibility(View.VISIBLE);
//
//        final Button actiLoad = findViewById(R.id.bt_load);
//        actiLoad.setOnClickListener(v -> {
//            loadOTAsFile();
//        });

        final View containerDevi = findViewById(R.id.container_devi);
        containerDevi.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
        final TextView deviTitl = containerDevi.findViewById(R.id.title);
        deviTitl.setText(R.string.device_name);
        final TextView deviView = containerDevi.findViewById(R.id.text);
        deviView.setVisibility(View.VISIBLE);

        final View containerAddr = findViewById(R.id.container_addr);
        containerAddr.findViewById(R.id.image).
                setBackground(ContextCompat.getDrawable(this, R.drawable.ic_label_black_alpha_24dp));
        final TextView addrTitl = containerAddr.findViewById(R.id.title);
        addrTitl.setText(R.string.device_addr);
        final TextView addrView = containerAddr.findViewById(R.id.text);
        addrView.setVisibility(View.VISIBLE);

        final Button actiSlct = findViewById(R.id.bt_slct);
        actiSlct.setOnClickListener(v -> {
            slctOTAsDevi();
        });

        final ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            if (TextUtils.isEmpty(mAddr)) {
                displaySlctDeviSnackBar();
            } else {
                if (null != mOtasMngr) mOtasMngr = null;

                mOtasMngr = new PhyPPSPMngr(getApplicationContext(), this, mMsgsHdlr);
                mOtasMngr.opnLink(mAddr);


            }
        });

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getScrollY() == 0) {
                fab.extend();
            } else {
                fab.shrink();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsActivityResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsActivityResumed = false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

//    @Override
//    public void requestPermission() {
//        Utils.markWriteStoragePermissionRequested(this);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_STORAGE_PERMISSION) {
//            if (PackageManager.PERMISSION_GRANTED != grantResults[0]) {
//                Toast.makeText(this, getString(R.string.ext_storage_permission_denied), Toast.LENGTH_LONG).show();
//            }
//        }
//    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if (requestCode == READ_FILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uris = data.getData();
                    final String[] arry = uris.getPath().split(":");
                    if (arry.length == 2) {
                        final String type = arry[0];
                        final String path = arry[1];

                        mFile = new File(Environment.getExternalStorageDirectory() + "/" + path);

                        ((TextView) mCardFile.findViewById(R.id.container_file).findViewById(R.id.text)).setText(mFile.getName());
//                        ((TextView) mCardFile.findViewById(R.id.container_type).findViewById(R.id.text)).setText(file.getName());
                        ((TextView) mCardFile.findViewById(R.id.container_size).findViewById(R.id.text)).setText(String.format(Locale.US, "%d Bytes", mFile.length()));
                    }
                }
            } else {
                Log.e(TAG, "Error while opening file browser");
            }
        } else */
        if (SLCT_NODE_REQUEST_CODE == requestCode) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    ExtendedBluetoothDevice devi = data.getParcelableExtra(Utils.EXTRA_DEVICE);
                    mAddr = devi.getAddress();
                    ((TextView) mCardNode.findViewById(R.id.container_devi).findViewById(R.id.text)).setText(devi.getName());
                    ((TextView) mCardNode.findViewById(R.id.container_addr).findViewById(R.id.text)).setText(devi.getAddress());
                }
            }
        }
    }

    @Override
    public void onOtasCompleted() {
    }

    /*@Override
    public void onException(int code) {
        Log.d(TAG, "onException: " + code);
        if (mIsActivityResumed)
            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_OTAS_EXCEPTION) == null) {
                DialogFragmentError fragmentOtasError = DialogFragmentError.
                        newInstance(getString(R.string.title_error), "");
                fragmentOtasError.show(getSupportFragmentManager(), DIALOG_FRAGMENT_OTAS_EXCEPTION);
            }
    }*/

    /*@Override
    public void onProgress(float percentage) {
        //Log.d(TAG, "onProgress: " + percentage);
    }*/

    /*@Override
    public void onStatus(int status) {
        runOnUiThread(() -> {
            if (StatusCode.ConnAppl == status) showProgressbar();
            else if (StatusCode.OtasRset == status) hideProgressBar();
            else if (StatusCode.ErrsStat == status) hideProgressBar();
        });
    }*/

    /*@Override
    public void onComplete() {
        if (mIsActivityResumed)
            if (getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_OTAS_COMPLETED) == null) {
                DialogFragmentOtasComplete fragmentOtasComplete = DialogFragmentOtasComplete.
                        newInstance(getString(R.string.title_firmware_update), getString(R.string.summary_firmware_update_complete));
                fragmentOtasComplete.show(getSupportFragmentManager(), DIALOG_FRAGMENT_OTAS_COMPLETED);
            }
    }*/

    /*private void displayLoadFileSnackBar() {
        Snackbar.make(container, getString(R.string.msgs_file_unavailable), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.acti_load_file), view -> loadOTAsFile())
                .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                .show();
    }*/

    private void displaySlctDeviSnackBar() {
        Snackbar.make(container, getString(R.string.msgs_devi_unavailable), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.acti_slct_devi), view -> slctOTAsDevi())
                .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                .show();
    }

    @SuppressWarnings("UnusedReturnValue")
    /*private void loadOTAsFile() {
        performFileSearch();
    }*/

    private void slctOTAsDevi() {
        final Intent intent = new Intent(this, OTAsScanActivity.class);
        startActivityForResult(intent, SLCT_NODE_REQUEST_CODE);
    }

    /**
     * Fires an intent to spin up the "file chooser" UI to select a file
     */
//    private void performFileSearch() {
//        final Intent intent;
//        if (Utils.isKitkatOrAbove()) {
//            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        } else {
//            intent = new Intent(Intent.ACTION_GET_CONTENT);
//        }
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
//        startActivityForResult(intent, READ_FILE_REQUEST_CODE);
//    }
    protected final void showProgressbar() {
        disableClickableViews();
        mProgressbar.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected final void hideProgressBar() {
        enableClickableViews();
        mProgressbar.setVisibility(View.INVISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void enableClickableViews() {
        final Button actiLoad = findViewById(R.id.bt_load);
        final Button actiSlct = findViewById(R.id.bt_slct);
        final ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);

        actiLoad.setEnabled(true);
        actiSlct.setEnabled(true);
        fab.setEnabled(true);
    }

    protected void disableClickableViews() {
        final Button actiLoad = findViewById(R.id.bt_load);
        final Button actiSlct = findViewById(R.id.bt_slct);
        final ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);

        actiLoad.setEnabled(false);
        actiSlct.setEnabled(false);
        fab.setEnabled(false);
    }

    @Override
    public void onScan() {

    }

    @Override
    public void onConn() {

    }

    @Override
    public void onDisc() {

    }

    @Override
    public void onEnum() {

    }
    Handler hdlr = new Handler();

    @Override
    public void onLinkOpen() {
        Log.d(TAG, "onLinkOpen: >>>>");

        hdlr.postDelayed(new Runnable() {
            @Override
            public void run() {
                ByteBuffer byteBuffer = ByteBuffer.allocate(40);
//                byteBuffer.put(new byte[]{0x22, 0x01, (byte) mGKeyPosi,});
//                byteBuffer.put((byte) ((group.getAddress()>>0)&0xff));
//                byteBuffer.put((byte) ((group.getAddress()>>8)&0xff));
                mMsgsHdlr.sndMISRMsgs(byteBuffer.array());
            }
        }, 2000);
    }

    @Override
    public void onLinkClos() {

    }
}
