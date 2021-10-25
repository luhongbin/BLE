# Android-蓝牙控制LED灯群组

## 简介

本项目采用作为Bluetooth Mesh规范的蓝牙控制群组的LED灯的功能实现...与智能监控[monitorApp](https://github.com/luhongbin/monitorApp)的差异是:本项目是通过蓝牙控制 而且是群组...后者是通过WIFI控制单个LED灯

运行Android 4.3及以上版本的Android设备支持nRF Mesh for Android

### 特点
1.支持使用OOB数字资源调配
2.添加应用程序密钥
3.将添加的应用程序密钥绑定到模型
4.设置发布地址
5.订阅/取消订阅组地址和组地址
6.支持群组的基于蓝牙连续触发控制的 LED灯的设置

## 开发环境

* Android Studio
* An Android device with BLE capabilities

## 选项

* 基于nrf52832的开发工具包 用于测试样本固件

## 安装

*Android Studio直接打开项目
*连接Android设备。
*构建并运行项目。

## 项目代码

* [GitHub](https://github.com/luhongbin/BLE)

## 技术栈

> 1. android

## 致谢

本项目基于或参考以下项目：

1. [Android-nRF-Mesh](https://github.com/NordicSemiconductor/Android-nRF-Mesh-Library)

   项目介绍：蓝牙网络配置器和配置器库

2. [Android-BLE-Library](https://github.com/NordicSemiconductor/Android-BLE-Library)

   项目介绍： 一一个 Android 库 解决了很多 Android 的蓝牙低功耗问题 该BleManager 类暴露高层的API连接 并与蓝牙LE外围设备进行通信

3. [HomeApp](https://github.com/Domi04151309/HomeApp)

   项目介绍：一个小型的智能家居框架

4. [WiFi-led-control](https://github.com/NelisG/WiFi-led-control)

   项目介绍：用轮子调节LED等颜色变化

## 警告

> 1. 本项目仅用于学习练习
> 2. 本项目还不完善，仍处在开发中，不承担任何使用后果
> 3. 本项目代码开源[MIT](./LICENSE)，项目文档采用 [署名-禁止演绎 4.0 国际协议许可](https://creativecommons.org/licenses/by-nd/4.0/deed.zh)

## 问题

* 开发者有问题或者好的建议可以用Issues反馈交流，请给出详细信息
* 在开发交流群中应讨论开发、业务和合作问题
* QQ技术支持群657774700里提问，请在提问前先完成以下过程：
    * 请阅读[提问的智慧](https://github.com/ryanhanwu/How-To-Ask-Questions-The-Smart-Way/blob/master/README-zh_CN.md)；
    * 请百度或谷歌相关技术；
    * 请查看相关技术的官方文档；
    * 请提问前尽可能做一些DEBUG或者思考分析，然后提问时给出详细的错误相关信息以及个人对问题的理解。

## License

[MIT](https://github.com/luhongbin/BLE/blob/master/LICENSE)
Copyright (c) 2021-present luhongbin