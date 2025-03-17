#
# Copyright (C) 2023 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from device makefile.
$(call inherit-product, device/xiaomi/duchamp/device.mk)

# Inherit some common PixelAge stuff.
$(call inherit-product, vendor/pixelage/config/common_full_phone.mk)

PRODUCT_NAME := pixelage_duchamp
PRODUCT_DEVICE := duchamp
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := POCO
PRODUCT_MODEL := 2311DRK48G
PRODUCT_SYSTEM_NAME := duchamp_global

PRODUCT_CHARACTERISTICS := nosdcard

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=POCO/duchamp_global/duchamp:14/UP1A.230905.011/OS2.0.103.0.VNLMIXM:user/release-keys \
    DeviceProduct=$(PRODUCT_SYSTEM_NAME)

# Pixelage Flags
TARGET_FACE_UNLOCK_SUPPORTED := true
PIXELAGE_MAINTAINER := zenin1504
