cd hardware/lineage/compat
git fetch https://github.com/xiaomi-mt6897-duchamp/android_hardware_lineage_compat
git cherry-pick 9a046ea7e5899adc38ab04fe24eb34859fe4d779
cd ../../../

cd system/nfc
git fetch https://github.com/LineageOS/android_system_nfc
git cherry-pick b158cd56ae13bbed9cdbfdfe40e9fe5133892b83
cd ../../
