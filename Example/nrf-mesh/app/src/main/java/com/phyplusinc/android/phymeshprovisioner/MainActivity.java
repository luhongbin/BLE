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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import butterknife.ButterKnife;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import com.phyplusinc.android.phymeshprovisioner.di.Injectable;
import com.phyplusinc.android.phymeshprovisioner.utils.Utils;
import com.phyplusinc.android.phymeshprovisioner.viewmodels.SharedViewModel;

public class MainActivity extends AppCompatActivity implements Injectable,
        HasAndroidInjector,
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener {

    private static final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    @Inject
    DispatchingAndroidInjector<Object> mDispatchingAndroidInjector;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private NetworkFragment mNetworkFragment;
    private GroupsFragment mGroupsFragment;
    private ProxyFilterFragment mProxyFilterFragment;
    private Fragment mSettingsFragment;
    private SharedViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);//窗口重建时会把该窗口关闭以前的状态引入（关闭前保存）参数
        setContentView(R.layout.activity_main);
        mViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        ButterKnife.bind(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.app_name);
        //四选一，开机默认network
        mNetworkFragment = (NetworkFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_network);
        mGroupsFragment = (GroupsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_groups);
        mProxyFilterFragment = (ProxyFilterFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_proxy);
        mSettingsFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_settings);

        //底部导航栏控件（有单独的菜单资源文件xml）
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);

        bottomNavigationView.setOnNavigationItemSelectedListener(this);//选中监听
        bottomNavigationView.setOnNavigationItemReselectedListener(this);//

        //选择上次关闭前显示的窗口，如果是第一次，显示network
        if (savedInstanceState == null) {
            onNavigationItemSelected(bottomNavigationView.getMenu().findItem(R.id.action_network));
        } else {
            bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(CURRENT_FRAGMENT));
        }
    }
    //判断是否已经连接上代理节点
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final Boolean isConnectedToNetwork = mViewModel.isConnectedToProxy().getValue();
        if (isConnectedToNetwork != null && isConnectedToNetwork) {
            getMenuInflater().inflate(R.menu.disconnect, menu);
        } else {
            getMenuInflater().inflate(R.menu.connect, menu);
        }
        return true;
    }

    //连接按钮监听
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                mViewModel.navigateToScannerActivity(this, false, Utils.CONNECT_TO_NETWORK, false);
                //navigateTo跳转
                return true;
            case R.id.action_disconnect:
                mViewModel.disconnect();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    //底部选项监听
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();//开启一个事务
        switch (id) {
            case R.id.action_network:
                ft.show(mNetworkFragment).hide(mGroupsFragment).hide(mProxyFilterFragment).hide(mSettingsFragment);
                break;
            case R.id.action_groups:
                ft.hide(mNetworkFragment).show(mGroupsFragment).hide(mProxyFilterFragment).hide(mSettingsFragment);
                break;
            case R.id.action_proxy:
                ft.hide(mNetworkFragment).hide(mGroupsFragment).show(mProxyFilterFragment).hide(mSettingsFragment);
                break;
            case R.id.action_settings:
                ft.hide(mNetworkFragment).hide(mGroupsFragment).hide(mProxyFilterFragment).show(mSettingsFragment);
                break;
        }
        ft.commit();
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return mDispatchingAndroidInjector;
    }
}
