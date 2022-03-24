package nl.tudelft.trustchain.atomicswap.swap.eth;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
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
    public static final String BINARY = "608060405234801561001057600080fd5b50610ddc806100206000396000f3fe60806040526004361061003f5760003560e01c80633da0e66e1461004457806384cc9dfb1461008157806396afb365146100aa578063b7418983146100d3575b600080fd5b34801561005057600080fd5b5061006b600480360381019061006691906108e5565b6100ef565b60405161007891906109c1565b60405180910390f35b34801561008d57600080fd5b506100a860048036038101906100a391906109dc565b6101db565b005b3480156100b657600080fd5b506100d160048036038101906100cc91906108e5565b610483565b005b6100ed60048036038101906100e89190610a74565b6106d1565b005b6100f7610856565b600080838152602001908152602001600020604051806080016040529081600082015481526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016002820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016003820154815250509050919050565b6101e3610856565b600080838152602001908152602001600020604051806080016040529081600082015481526020016001820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016002820160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016003820154815250509050816002846040516020016102d69190610ae8565b6040516020818303038152906040526040516102f29190610b7d565b602060405180830381855afa15801561030f573d6000803e3d6000fd5b5050506040513d601f19601f820116820180604052508101906103329190610ba9565b1415610479573373ffffffffffffffffffffffffffffffffffffffff16816020015173ffffffffffffffffffffffffffffffffffffffff161461037457600080fd5b806020015173ffffffffffffffffffffffffffffffffffffffff166108fc82600001519081150290604051600060405180830381858888f193505050501580156103c2573d6000803e3d6000fd5b506000808381526020019081526020016000206000808201600090556001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690556002820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905560038201600090555050817f4c5d6ac9ed6c7c3efeb38cfa16a24e7a6f09616cf8a8f5d9624a499ace6d293f84836000015160405161046c929190610bf4565b60405180910390a261047e565b600080fd5b505050565b60008082815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610526576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161051d90610c7a565b60405180910390fd5b60008082815260200190815260200160002060030154431161057d576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161057490610ce6565b60405180910390fd5b60008082815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc600080848152602001908152602001600020600001549081150290604051600060405180830381858888f1935050505015801561060d573d6000803e3d6000fd5b506000808281526020019081526020016000206000808201600090556001820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690556002820160006101000a81549073ffffffffffffffffffffffffffffffffffffffff021916905560038201600090555050807fa61d93f0a0b2ced4cbccef66172f6b5734ccb7ece92133d6910915bf3b0bc264600080848152602001908152602001600020600001546040516106c69190610d06565b60405180910390a250565b600080600084815260200190815260200160002060030154146106f357600080fd5b60405180608001604052803481526020018473ffffffffffffffffffffffffffffffffffffffff1681526020013373ffffffffffffffffffffffffffffffffffffffff16815260200182436107489190610d50565b8152506000808481526020019081526020016000206000820151816000015560208201518160010160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060408201518160020160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550606082015181600301559050508273ffffffffffffffffffffffffffffffffffffffff16827f39102f719f4b29de188d61bc7fbfc0180fd7e3a5a03bd1b766c7d3e1931e96b1346040516108499190610d06565b60405180910390a3505050565b604051806080016040528060008152602001600073ffffffffffffffffffffffffffffffffffffffff168152602001600073ffffffffffffffffffffffffffffffffffffffff168152602001600081525090565b600080fd5b6000819050919050565b6108c2816108af565b81146108cd57600080fd5b50565b6000813590506108df816108b9565b92915050565b6000602082840312156108fb576108fa6108aa565b5b6000610909848285016108d0565b91505092915050565b6000819050919050565b61092581610912565b82525050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006109568261092b565b9050919050565b6109668161094b565b82525050565b608082016000820151610982600085018261091c565b506020820151610995602085018261095d565b5060408201516109a8604085018261095d565b5060608201516109bb606085018261091c565b50505050565b60006080820190506109d6600083018461096c565b92915050565b600080604083850312156109f3576109f26108aa565b5b6000610a01858286016108d0565b9250506020610a12858286016108d0565b9150509250929050565b610a258161094b565b8114610a3057600080fd5b50565b600081359050610a4281610a1c565b92915050565b610a5181610912565b8114610a5c57600080fd5b50565b600081359050610a6e81610a48565b92915050565b600080600060608486031215610a8d57610a8c6108aa565b5b6000610a9b86828701610a33565b9350506020610aac868287016108d0565b9250506040610abd86828701610a5f565b9150509250925092565b6000819050919050565b610ae2610add826108af565b610ac7565b82525050565b6000610af48284610ad1565b60208201915081905092915050565b600081519050919050565b600081905092915050565b60005b83811015610b37578082015181840152602081019050610b1c565b83811115610b46576000848401525b50505050565b6000610b5782610b03565b610b618185610b0e565b9350610b71818560208601610b19565b80840191505092915050565b6000610b898284610b4c565b915081905092915050565b600081519050610ba3816108b9565b92915050565b600060208284031215610bbf57610bbe6108aa565b5b6000610bcd84828501610b94565b91505092915050565b610bdf816108af565b82525050565b610bee81610912565b82525050565b6000604082019050610c096000830185610bd6565b610c166020830184610be5565b9392505050565b600082825260208201905092915050565b7f4d75737420626520737761702063726561746f72000000000000000000000000600082015250565b6000610c64601483610c1d565b9150610c6f82610c2e565b602082019050919050565b60006020820190508181036000830152610c9381610c57565b9050919050565b7f52656c6174697665206c6f636b206d7573742062652073617469736669656400600082015250565b6000610cd0601f83610c1d565b9150610cdb82610c9a565b602082019050919050565b60006020820190508181036000830152610cff81610cc3565b9050919050565b6000602082019050610d1b6000830184610be5565b92915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610d5b82610912565b9150610d6683610912565b9250827fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03821115610d9b57610d9a610d21565b5b82820190509291505056fea2646970667358221220cd81de6b4cb080d23823fbc9c40a32ff1c9bbcabda3e259b4fea28d73db98b5e64736f6c634300080c0033";

    public static final String FUNC_ADDSWAP = "addSwap";

    public static final String FUNC_CLAIM = "claim";

    public static final String FUNC_GETSWAP = "getSwap";

    public static final String FUNC_RECLAIM = "reclaim";

    public static final Event SWAPADDED_EVENT = new Event("swapAdded",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SWAPCLAIMED_EVENT = new Event("swapClaimed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SWAPRECLAIMED_EVENT = new Event("swapReclaimed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

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

    public List<SwapAddedEventResponse> getSwapAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SWAPADDED_EVENT, transactionReceipt);
        ArrayList<SwapAddedEventResponse> responses = new ArrayList<SwapAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SwapAddedEventResponse typedResponse = new SwapAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SwapAddedEventResponse> swapAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SwapAddedEventResponse>() {
            @Override
            public SwapAddedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SWAPADDED_EVENT, log);
                SwapAddedEventResponse typedResponse = new SwapAddedEventResponse();
                typedResponse.log = log;
                typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SwapAddedEventResponse> swapAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SWAPADDED_EVENT));
        return swapAddedEventFlowable(filter);
    }

    public List<SwapClaimedEventResponse> getSwapClaimedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SWAPCLAIMED_EVENT, transactionReceipt);
        ArrayList<SwapClaimedEventResponse> responses = new ArrayList<SwapClaimedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SwapClaimedEventResponse typedResponse = new SwapClaimedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.secret = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SwapClaimedEventResponse> swapClaimedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SwapClaimedEventResponse>() {
            @Override
            public SwapClaimedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SWAPCLAIMED_EVENT, log);
                SwapClaimedEventResponse typedResponse = new SwapClaimedEventResponse();
                typedResponse.log = log;
                typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.secret = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SwapClaimedEventResponse> swapClaimedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SWAPCLAIMED_EVENT));
        return swapClaimedEventFlowable(filter);
    }

    public List<SwapReclaimedEventResponse> getSwapReclaimedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SWAPRECLAIMED_EVENT, transactionReceipt);
        ArrayList<SwapReclaimedEventResponse> responses = new ArrayList<SwapReclaimedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SwapReclaimedEventResponse typedResponse = new SwapReclaimedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SwapReclaimedEventResponse> swapReclaimedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SwapReclaimedEventResponse>() {
            @Override
            public SwapReclaimedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SWAPRECLAIMED_EVENT, log);
                SwapReclaimedEventResponse typedResponse = new SwapReclaimedEventResponse();
                typedResponse.log = log;
                typedResponse.hashValue = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SwapReclaimedEventResponse> swapReclaimedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SWAPRECLAIMED_EVENT));
        return swapReclaimedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addSwap(String recipient, byte[] hashValue, BigInteger relativeLock, BigInteger weiValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADDSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, recipient),
                new org.web3j.abi.datatypes.generated.Bytes32(hashValue),
                new org.web3j.abi.datatypes.generated.Uint256(relativeLock)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function,weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> claim(byte[] preimage, byte[] hash) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(preimage),
                new org.web3j.abi.datatypes.generated.Bytes32(hash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Swap> getSwap(byte[] hashValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashValue)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Swap>() {}));
        return executeRemoteCallSingleValueReturn(function, Swap.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reclaim(byte[] hashValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
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
            super(new org.web3j.abi.datatypes.generated.Uint256(amount),new org.web3j.abi.datatypes.Address(recipient),new org.web3j.abi.datatypes.Address(reclaimer),new org.web3j.abi.datatypes.generated.Uint256(reclaim_height));
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

    public static class SwapAddedEventResponse extends BaseEventResponse {
        public byte[] hashValue;

        public String recipient;

        public BigInteger amount;
    }

    public static class SwapClaimedEventResponse extends BaseEventResponse {
        public byte[] hashValue;

        public byte[] secret;

        public BigInteger amount;
    }

    public static class SwapReclaimedEventResponse extends BaseEventResponse {
        public byte[] hashValue;

        public BigInteger amount;
    }
}
