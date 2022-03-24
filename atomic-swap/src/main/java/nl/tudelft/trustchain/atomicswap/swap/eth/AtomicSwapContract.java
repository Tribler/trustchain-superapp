package nl.tudelft.trustchain.atomicswap.swap.eth;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class AtomicSwapContract extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50610ca0806100206000396000f3fe60806040526004361061003f5760003560e01c80633da0e66e1461004457806384cc9dfb1461008157806396afb365146100aa578063b7418983146100d3575b600080fd5b34801561005057600080fd5b5061006b6004803603810190610066919061080b565b6100ef565b60405161007891906108e7565b60405180910390f35b34801561008d57600080fd5b506100a860048036038101906100a39190610902565b6101db565b005b3480156100b657600080fd5b506100d160048036038101906100cc919061080b565b610445565b005b6100ed60048036038101906100e8919061099a565b610646565b005b6100f761077c565b600080838152602001908152602001600020604051806080016040529081600082015481526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016002820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016003820154815250509050919050565b6101e361077c565b600080838152602001908152602001600020604051806080016040529081600082015481526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016002820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016003820154815250509050816002846040516020016102d69190610a0e565b6040516020818303038152906040526040516102f29190610aa3565b602060405180830381855afa15801561030f573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906103329190610acf565b141561043b573373ffffffffffffffffffffffffffffffffffffffff16816020015173ffffffffffffffffffffffffffffffffffffffff161461037457600080fd5b806020015173ffffffffffffffffffffffffffffffffffffffff166108fc82600001519081150290604051600060405180830381858888f193505050501580156103c2573d6000803e3d6000fd5b506000808381526020019081526020016000206000808201600090556001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690556002820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905560038201600090555050610440565b600080fd5b505050565b60008082815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146104e8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104df90610b59565b60405180910390fd5b60008082815260200190815260200160002060030154431161053f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161053690610bc5565b60405180910390fd5b60008082815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc600080848152602001908152602001600020600001549081150290604051600060405180830381858888f193505050501580156105cf573d6000803e3d6000fd5b506000808281526020019081526020016000206000808201600090556001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690556002820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690556003820160009055505050565b6000806000848152602001908152602001600020600301541461066857600080fd5b60405180608001604052803481526020018473ffffffffffffffffffffffffffffffffffffffff1681526020013373ffffffffffffffffffffffffffffffffffffffff16815260200182436106bd9190610c14565b8152506000808481526020019081526020016000206000820151816000015560208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060408201518160020160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060608201518160030155905050505050565b604051806080016040528060008152602001600073ffffffffffffffffffffffffffffffffffffffff168152602001600073ffffffffffffffffffffffffffffffffffffffff168152602001600081525090565b600080fd5b6000819050919050565b6107e8816107d5565b81146107f357600080fd5b50565b600081359050610805816107df565b92915050565b600060208284031215610821576108206107d0565b5b600061082f848285016107f6565b91505092915050565b6000819050919050565b61084b81610838565b82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061087c82610851565b9050919050565b61088c81610871565b82525050565b6080820160008201516108a86000850182610842565b5060208201516108bb6020850182610883565b5060408201516108ce6040850182610883565b5060608201516108e16060850182610842565b50505050565b60006080820190506108fc6000830184610892565b92915050565b60008060408385031215610919576109186107d0565b5b6000610927858286016107f6565b9250506020610938858286016107f6565b9150509250929050565b61094b81610871565b811461095657600080fd5b50565b60008135905061096881610942565b92915050565b61097781610838565b811461098257600080fd5b50565b6000813590506109948161096e565b92915050565b6000806000606084860312156109b3576109b26107d0565b5b60006109c186828701610959565b93505060206109d2868287016107f6565b92505060406109e386828701610985565b9150509250925092565b6000819050919050565b610a08610a03826107d5565b6109ed565b82525050565b6000610a1a82846109f7565b60208201915081905092915050565b600081519050919050565b600081905092915050565b60005b83811015610a5d578082015181840152602081019050610a42565b83811115610a6c576000848401525b50505050565b6000610a7d82610a29565b610a878185610a34565b9350610a97818560208601610a3f565b80840191505092915050565b6000610aaf8284610a72565b915081905092915050565b600081519050610ac9816107df565b92915050565b600060208284031215610ae557610ae46107d0565b5b6000610af384828501610aba565b91505092915050565b600082825260208201905092915050565b7f4d75737420626520737761702063726561746f72000000000000000000000000600082015250565b6000610b43601483610afc565b9150610b4e82610b0d565b602082019050919050565b60006020820190508181036000830152610b7281610b36565b9050919050565b7f52656c6174697665206c6f636b206d7573742062652073617469736669656400600082015250565b6000610baf601f83610afc565b9150610bba82610b79565b602082019050919050565b60006020820190508181036000830152610bde81610ba2565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610c1f82610838565b9150610c2a83610838565b9250827fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03821115610c5f57610c5e610be5565b5b82820190509291505056fea2646970667358221220c544f44caa628aafd1d309857cf3c7a8da7ec3c12cbf5b629159d19f19fc243164736f6c634300080c0033";

    public static final String FUNC_ADDSWAP = "addSwap";

    public static final String FUNC_CLAIM = "claim";

    public static final String FUNC_GETSWAP = "getSwap";

    public static final String FUNC_RECLAIM = "reclaim";

    @Deprecated
    protected AtomicSwapContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected AtomicSwapContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected AtomicSwapContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected AtomicSwapContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addSwap(String recipient, byte[] hashValue, BigInteger relativeLock, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_ADDSWAP,
                Arrays.<Type>asList(new Address(160, recipient),
                new org.web3j.abi.datatypes.generated.Bytes32(hashValue),
                new Uint256(relativeLock)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function,weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> claim(byte[] preimage, byte[] hash) {
        final Function function = new Function(
                FUNC_CLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(preimage),
                new org.web3j.abi.datatypes.generated.Bytes32(hash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Swap> getSwap(byte[] hashValue) {
        final Function function = new Function(FUNC_GETSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashValue)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Swap>() {}));
        return executeRemoteCallSingleValueReturn(function, Swap.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reclaim(byte[] hashValue) {
        final Function function = new Function(
                FUNC_RECLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashValue)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static AtomicSwapContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new AtomicSwapContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static AtomicSwapContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new AtomicSwapContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static AtomicSwapContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new AtomicSwapContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static AtomicSwapContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new AtomicSwapContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<AtomicSwapContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AtomicSwapContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AtomicSwapContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AtomicSwapContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<AtomicSwapContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AtomicSwapContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AtomicSwapContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AtomicSwapContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class Swap extends StaticStruct {
        public BigInteger amount;

        public String recipient;

        public String reclaimer;

        public BigInteger reclaim_height;

        public Swap(BigInteger amount, String recipient, String reclaimer, BigInteger reclaim_height) {
            super(new Uint256(amount),new Address(recipient),new Address(reclaimer),new Uint256(reclaim_height));
            this.amount = amount;
            this.recipient = recipient;
            this.reclaimer = reclaimer;
            this.reclaim_height = reclaim_height;
        }

        public Swap(Uint256 amount, Address recipient, Address reclaimer, Uint256 reclaim_height) {
            super(amount,recipient,reclaimer,reclaim_height);
            this.amount = amount.getValue();
            this.recipient = recipient.getValue();
            this.reclaimer = reclaimer.getValue();
            this.reclaim_height = reclaim_height.getValue();
        }
    }
}
