# TrustChain Voter
This submodule of the TrustChain Super App serves to demonstrate functionality that is provided by the Voting API (`common/util/Votinghelper`). For documentation on the API itself, please see [this page](../common/README.md#votinghelper). 

This module currently contains the following functionality:
- Retrieve and show list of current proposals including current tally and voting progress
- Cast a vote on a proposal
- Create a new proposal in Yes/No voting mode or threshold voting mode
- `EXPERIMENTAL` Freedom of Computing file proposal

## Context
As mentioned before, this module's sole purpose is to demonstrate the usage of the [Voting API](../common/README.md#votinghelper). The Voting API should be used within the context of a Community or even DAO. Within this community, peers will have the possibility to create proposals, cast votes and see the results of the voting. Below, we will elaborate on the different capabilities of the TrustChain Voter. Furthermore, a proof of concept which combines the TrustChain Voter functionality with FOC can be found at the end.

## Proposal list
When entering the submodule, the user sees the proposal list at first. Using the toggle, the user can switch between a list of all proposals and a list of new proposals, which contains proposals on which the user has not casted a vote yet. The purpose of this is to allow the user for a clear view of which proposals are still waiting to have a vote cast upon.

Every proposal in the list has a progress bar which indicates how many votes have been cast in total (Yes/No proposal) or how many Yes votes have been cast (threshold proposal). There is also an indicator which indicates whether the user has not cast a vote yet (`NEW`), has cast a voted (`VOTED`) or when the voting has been completed (`COMPLETED`).

<img src="https://user-images.githubusercontent.com/17474698/80521306-1b516500-898b-11ea-9f02-f982db4c3cde.gif" width="280"> 

*User toggling between the two modes*

## Creating a new proposal
A user can create a new proposal by clicking on the `create proposal` button in the bottom right corner. A dialog will pop up in which the user can enter a name for the proposal and switch between Yes/No voting and threshold voting. Once the proposal has been created, it directly pops up in the proposal list of peers that are in the community.

<img src="https://user-images.githubusercontent.com/17474698/80522657-37560600-898d-11ea-9dbb-eeff8eec9ea2.gif" width="280">

*User creating a new proposal to be voted upon*

### Yes/No voting mode
In the Yes/No voting mode, the result of the proposal is determined by counting the number of Yes and No votes. When inspecting a proposal of this mode, the user is presented the option to vote Yes or No, as well as relevant standings of the vote.

<img src="https://user-images.githubusercontent.com/17474698/80524305-b2b8b700-898f-11ea-99f3-6225eb3fe705.png" width="280">

*User being presented with the option to cast a vote on a Yes/No vote proposal*

### Threshold voting mode
In the threshold voting mode, the result of the proposal is determined by deciding whether the number of Yes votes reaches a certain threshold. This threshold is decided upon when the community or DAO is created and by default is `75%`. When inspecting a proposal of this mode, the user is presented with the option to agree (or not cast a vote, as a way of disagreeing), as well as the relevant standings of the vote.

<img src="https://user-images.githubusercontent.com/17474698/80524307-b3514d80-898f-11ea-9965-9c9a3268b0b9.png" width="280">

*User being presented with the option to cast a vote on a threshold vote proposal*

## Casting vote
When clicking on a proposal, the user will see a new dialog pop up which he/she can use to cast a vote if he/she has not done so before. Depending on the voting mode of the proposal, the user can cast a Yes or No vote (Yes/No mode), agree with a proposal (threshold mode) or do nothing by pressing the `Cancel` button.

In the case that the user has already cast a vote on the selected proposal, an overview of the proposal and the user's vote will be shown.

<img src="https://user-images.githubusercontent.com/17474698/80523868-08d92a80-898f-11ea-8b39-4967a5915a27.gif" width="280"> 

*User going through the process of casting a vote*

## `EXPERIMENTAL` Freedom of Computing file proposal
Mostly as a proof of concept, and a demonstration of another actual use case, one can propose a file to be uploaded using the functionality of the Freedom of Computing group. This can be done through proposing the filepath of the relevant file in the TrustChain Voter. Only when the proposal has been accepted by the community or DAO, this will be available for uploading. As this is experimental, and a proof of concept, this functionality has not been included in the main development branch. For those interested, the implementation can be found [here](https://github.com/emieldesmidt/trustchain-superapp/tree/FOC-Integration)

<img src="https://user-images.githubusercontent.com/17474698/80526174-a2560b80-8992-11ea-96cd-7089d7b47e29.gif" width="280">

*User going through the process of proposing a file to upload, and once it has been accepted by the community, going to the FOC app and find it as an available option*
