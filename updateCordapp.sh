./gradlew clean assemble cpk

echo "cleaning cpk folder"
rm -rf cpks 

mkdir ./cpks

echo "copying files over"
cp -rf contracts/build/libs/corda5-interop-test-cordapp-contracts-1.0-SNAPSHOT-cordapp.cpk ./cpks

cp -rf workflows/build/libs/corda5-interop-test-cordapp-workflows-1.0-SNAPSHOT-cordapp.cpk ./cpks

corda-cli package install -n fruit-trading cpks\