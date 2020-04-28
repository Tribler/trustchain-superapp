# TrustChain common module
This module contains functionality and resources that is used by multiple other submodules of the TrustChain Super App.

## Code
Under `src/main/java/nl/tudelft/trustchain/common` multiple folders and files can be found that contain functionalities or constants.

### VotingHelper
This file contains the functions that form the voting API, which is used in the TrustChain Voter submodule. These functions can be accessed by first instantiating the VotingHelper with a community e.g. `val vh: = VotingHelper(community)`. It is recommended to instantiate the VotingHelper as a global variable in your class so that its functions can be accessed throughout the class. An example of its use can be found in the `VotingActivity.kt` file within the TrustChain Voter submodule.

Below, short descriptions of the available functions in VotingHelper are given.

#### Creating a proposal
```
startVote(voteSubject: String, peers: List<PublicKey>, mode: VotingMode)
```
A proposal is created using the`startVote` method. This method requires a name for the proposal and a list of peers that are allowed to vote on the proposal. Usually, all the peers which are currently in the community should be in this list. Finally, a `VotingMode` can be specified, indicating a threshold vote (`VotingMode.THRESHOLD`) or a yes/no vote (`VotingMode.YESNO`). A threshold vote is a vote where voters can only vote 'yes'; the vote is passed when enough people have voted. Note that in this case, not voting is different from voting 'NO'. More on the technical and security implications of voting in section xnxx.


#### Casting a vote
```
respondToVote(vote: Boolean, proposalBlock: TrustChainBlock)
```
A vote is cast using the `respondToVote` method. This method requires the vote as a Boolean (True = Yes, False = No) and the TrustChainBlock which contains the initial proposal block.

#### Counting votes
```
countVotes(voters: List<PublicKey>, voteSubject: String, proposerKey: ByteArray): Pair<Int, Int>
```

The current tally of a proposal can be retrieved using the `countVotes` method. It requires a list of peers that partake in the vote, the name of the proposal and the key of the proposer. The current tally of the proposal is returned as a pair: `(#Yes, #No)`.

#### Checking vote completeness
```
votingIsComplete(block: TrustChainBlock, threshold: Int = -1): Boolean
```

To check whether a vote is complete you can call the `votingIsComplete` function. It takes as argument the proposal block of the vote and a threshold and the function returns a boolean.

#### Checking vote progress
```
getVoteProgressStatus(block: TrustChainBlock, threshold: Int = -1): Int
```
To check the progress of a vote you can call the `getVotingProgressStatus` function. It takes as argument the proposalblock of the vote and a threshold and the function returns an integer. This integer is a percentage of the required votes or -1 in the case of a thresholdvote that received votes from all participants but of which the threshold was not met.

#### Check whether peer has voted
```
fun castedByPeer(block: TrustChainBlock, publicKey: PublicKey): Pair<Int, Int>
```
To check whether a certain peer has alreay voted `YES` or `NO` for  a certain vote, call `castedByPeer` with the proposalblock of the vote and the peer's public key. The function returns a `Pair<int,int>` representing the numbers of `yes-votes` and `no-votes`. 

## Resources
The `src/main/res` folder contains resources which are used for the front-end of the different submodules. 

## Tests
The `src/test` folder currently only contains tests for the `VotingHelper`.
