# TrustChain common module
This module contains functionality and resources that is used by multiple other submodules of the TrustChain Super App.

## Code
Under `src/main/java/nl/tudelft/trustchain/common` multiple folders and files can be found that contain functionalities or constants.

### VotingHelper
This file contains the functions that form the voting API, which is used in the TrustChain Voter submodule. These functions can be accessed by first instantiating the VotingHelper with a community e.g. `val vh: = VotingHelper(community)`. It is recommended to instantiate the VotingHelper as a global variable in your class so that its functions can be accessed throughout the class. An example of its use can be found in the `VotingActivity.kt` file within the TrustChain Voter submodule.

Below, short descriptions of the available functions in VotingHelper are given.

#### Creating a proposal
A proposal is created using the `startVote` method. This method requires a name for the proposal and a list of peers that are allowed to vote on the proposal. Usually, all the peers which are currently in the community should be in this list.

#### Casting a vote
A vote is cast using the `respondToVote` method. This method requires the vote as a Boolean (True = Yes, False = No) and the TrustChainBlock which contains the initial proposal block.

#### Counting votes
The current tally of a proposal can be retrieved using the `countVotes` method. It requires a list of peers that partake in the vote, the name of the proposal and the key of the proposer. The current tally of the proposal is returned as a pair: `(#Yes, #No)`.

## Resources
The `src/main/res` folder contains resources which are used for the front-end of the different submodules.

## Tests
The `src/test` folder currently only contains tests for the `VotingHelper`.
