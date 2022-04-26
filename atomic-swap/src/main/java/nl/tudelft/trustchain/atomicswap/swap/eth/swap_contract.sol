pragma solidity ^0.8.8;

contract AtomicSwap {

	struct Swap {
		uint256 amount;
		address recipient;
		address reclaimer; // person who can reclaim
		uint256 reclaim_height; // block
	}

	event swapAdded(
		bytes32 indexed hashValue,
		address indexed recipient,
		uint256 amount
	);

	event swapClaimed(
		bytes32 indexed hashValue,
		bytes32 secret,
		uint256 amount
	);

	event swapReclaimed(
		bytes32 indexed hashValue,
		uint256 amount
	);

	mapping(bytes32 => Swap) swaps;

	// Takes a hash and returns the swap details of that hash.
	function getSwap(bytes32 hashValue) public view returns (Swap memory swap){
		swap = swaps[hashValue];
	}
	// adds a swap. Note that the preimage of the hash must 32 bytes.
	function addSwap(address recipient, bytes32 hashValue, uint relativeLock) public payable {
		// use this to check if this hash was used.
		if (swaps[hashValue].reclaim_height != 0) {
			revert();
		}
		swaps[hashValue] = Swap(msg.value, recipient, msg.sender, block.number + relativeLock);
		emit swapAdded(hashValue,recipient,msg.value);
	}
	// reclaims the swap of a given hash if the relative lock is satisfied.
	function reclaim(bytes32 hashValue) public {

		require(msg.sender == swaps[hashValue].reclaimer, "Must be swap creator");
		require(block.number > swaps[hashValue].reclaim_height, "Relative lock must be satisfied");

		payable(swaps[hashValue].reclaimer).transfer(swaps[hashValue].amount);
		delete swaps[hashValue];
		emit swapReclaimed(hashValue,swaps[hashValue].amount);
	}

	// claim a swap if the hashed preimage is equal to the hash.
	function claim(bytes32 preimage, bytes32 hash) public {
		Swap memory swap;
		swap = swaps[hash];
		if (sha256(abi.encodePacked(preimage)) == hash) {
			if (swap.recipient != msg.sender) {
				revert();
			}
			payable(swap.recipient).transfer(swap.amount);
			delete swaps[hash];
			emit swapClaimed(hash,preimage,swap.amount);
		} else {
			revert();
		}
	}

}
