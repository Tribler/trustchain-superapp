# TrustChain common module
This module contains functionality and resources that are used by multiple other submodules of the TrustChain Super App.

## Code
Under `src/main/java/nl/tudelft/trustchain/common` multiple folders and files can be found that contain functionalities or constants.

### VotingHelper
This file contains the functions that form the Voting API, which is used in the TrustChain Voter submodule. These functions can be accessed by first instantiating the VotingHelper with a community e.g. `val vh: = VotingHelper(community)`. It is recommended to instantiate the VotingHelper as a global variable in your class so that its functions can be accessed throughout the class. An example of its use can be found in the [`VotingActivity.kt`](../trustchain-voter/src/main/java/nl/tudelft/trustchain/voting/VotingActivity.kt) file within the TrustChain Voter submodule.

Below, short descriptions of the available functions in VotingHelper are given.

#### Creating a proposal
```
createProposal(voteSubject: String, peers: List<PublicKey>, mode: VotingMode)
```
A proposal is created using the`createProposal` method. This method requires a name for the proposal and a list of peers that are allowed to vote on the proposal. Usually, all the peers which are currently in the community should be in this list. Finally, a `VotingMode` can be specified, indicating a threshold vote (`VotingMode.THRESHOLD`) or a yes/no vote (`VotingMode.YESNO`). A threshold vote is a vote where voters can only vote 'yes'; the vote is passed when enough people have voted. Note that in this case, not voting is different from voting 'NO'. More on the technical and security implications of voting can be found in [this section](#some-notes-on-security).


#### Casting a vote
```
respondToProposal(vote: Boolean, proposalBlock: TrustChainBlock)
```
A vote is cast using the `respondToProposal` method. This method requires the vote as a `Boolean` (True = Yes, False = No) and the `TrustChainBlock` which contains the initial proposal block.

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
getVotingProgressStatus(block: TrustChainBlock, threshold: Int = -1): Int
```
To check the progress of a vote you can call the `getVotingProgressStatus` function. It takes as argument the proposalblock of the vote and a threshold and the function returns an integer. This integer is a percentage of the required votes or -1 in the case of a thresholdvote that received votes from all participants but of which the threshold was not met.

#### Check whether peer has voted
```
fun castedByPeer(block: TrustChainBlock, publicKey: PublicKey): Pair<Int, Int>
```
To check whether a certain peer has already voted `YES` or `NO` for  a certain vote, call `castedByPeer` with the proposalblock of the vote and the peer's public key. The function returns a Pair<int,int> representing the numbers of `yes-votes` and `no-votes`.

#### Some notes on security
The voting API currently allows for two types of voting; threshold and yes/no.

- Yes/No:
The process of creating a vote on the TrustChain is rather primitive. One proposer creates a multi-signature block with a subject, a type, and a list of eligible voters.

When starting a vote, a list of public keys is used to determine what peers are entitled to participate in the vote. This list is used to determine the outcome of the vote, so when a peer that was not included in the vote still votes for some reason, its vote is not accounted for in the result. This mechanism ensures for example that peers that join the dao after a vote was proposed, are not able to vote.

The multi-signature block is sent to all participants in a community. Voters can vote by responding to the proposer's half-block with another half-block containing a `YES` or a `NO`. There are a couple of problems with this implementation.

First of all, the proposer would be able to hide the entire vote if the outcome is not in its favour. This block-hiding could be detected by crawling the chains of the eligible voters, but the current implementation does not do so. Secondly, voters' voters are only stored on the proposer's and the voter's chain. This means that if both were to remove their chains, the outcome of the vote cannot be retrieved. Finally, it is assumed that the proposer stays available during the voting process.

In short, yes/no voting should really only be used, if ever, in case of a trusted proposer.

- Threshold

When working with thresholds, a proposer no longer has an incentive to hide his voting blocks. A proposer starts a vote to pass his proposal and if its proposal doesn't get enough yes votes, hiding his blocks will not help him to pass his proposal.

Stalling is also not relevant anymore, because the vote is finished after a certain threshold is passed, and if enough people simply do not vote, this threshold will not be met.

## Resources
The `src/main/res` folder contains resources which are used for the front-end of the different submodules. 
## Tests
The `src/test` folder currently only contains tests for the `VotingHelper`.
