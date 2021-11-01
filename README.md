# Corda 5 Fruit Trading

A simple Corda 5 Cordapp that to test modified Corda 5 notary with signature verification


## Flows

There are two flows `ExchangeFruitFlow` which takes the following parameters:

- `receiver` (X500 String) - The party you want to trade with
- `gives` (String) - The fruit type you want to offer (APPLE or BANANA)
- `given_quantity` (Int) - How many fruits you want to give
- `wants` (String) - The fruit type you want to receive
- `wanted_quantity` (Int) - How many fruits you want to receive
- `message` (String) - A message to send with the transaction

And `GiveAwayFlow` which the following parameters

- `receivers` (list of X500 String) - The list of parties receiving the fruits
- `fruitType` (String) - The fruit type you want to offer (APPLE or BANANA)
- `quantity` (Int) - How many fruits you want to give (divided between parties)
- `message` (String) - A message to send with the transaction

In addition to that, there are faulty version of these 2 flows where the FinalityFlow doesn't propagate 
to other participant nodes

## Deploying and Testing
### Required Prerequisites

- Corda CLI
- Cordapp Builder
- Node CLI
- Docker

### Deployment via Corda CLI

1. Navigate to the app directory
2. Build the app with `gradlew build`
3. Build the Cordapp with the cordapp-builder CLI util `cordapp-builder create --cpk contracts\build\libs\<CPK FILE NAME> --cpk workflows\build\libs\<CPK FILE NAME> -o fruit-trading.cpb`
4. Configure the network with `corda-cli network config docker-compose fruit-trading`
5. Build the network deployment dockerfile using corda-cli `corda-cli network deploy -n solar-system -f solar-system.yaml > docker-compose.yaml`
6. Deploy the network using docker-compose `docker-compose -f docker-compose.yaml up -d`
7. When deployed check the status with corda-cli `corda-cli network status -n fruit-trading` note the mapped web ports for Http RPC
8. Install the application on the network using corda-cli `corda-cli package install -n fruit-trading fruit-trading.cpb`

### Testing VIA Swagger
- Using the port noted from the network status visit `https://localhost:<port>/api/v1/swagger`
- Login using the button on the top right usernames and passwords are as follows: `user1` and `password`
- Launch the `ExchangeFruitFault` via the Start Flow api 

