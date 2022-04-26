package nl.tudelft.trustchain.common.ethereum.contracts.web3j;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
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
import org.web3j.tuples.generated.Tuple4;
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
public class MultiSigWallet extends Contract {
    public static final String BINARY = "606060405234156200000d57fe5b604051620018163803806200181683398101604052805160208201519101905b600082518260328211806200004157508181115b806200004b575080155b8062000055575081155b15620000615760006000fd5b600092505b845183101562000136576002600086858151811015156200008357fe5b6020908102909101810151600160a060020a031682528101919091526040016000205460ff1680620000d657508483815181101515620000bf57fe5b90602001906020020151600160a060020a03166000145b15620000e25760006000fd5b6001600260008786815181101515620000f757fe5b602090810291909101810151600160a060020a03168252810191909152604001600020805460ff19169115159190911790555b60019092019162000066565b84516200014b9060039060208801906200015e565b5060048490555b5b5050505050620001f7565b828054828255906000526020600020908101928215620001b6579160200282015b82811115620001b65782518254600160a060020a031916600160a060020a039091161782556020909201916001909101906200017f565b5b50620001c5929150620001c9565b5090565b620001f491905b80821115620001c5578054600160a060020a0319168155600101620001d0565b5090565b90565b61160f80620002076000396000f300606060405236156101015763ffffffff60e060020a600035041663025e7c278114610153578063173825d91461018257806320ea8d86146101a05780632f54bf6e146101b55780633411c81c146101e557806354741525146102185780637065cb4814610244578063784547a7146102625780638b51d13f146102895780639ace38c2146102ae578063a0e67e2b1461036b578063a8abe69a146103d6578063b5dc40c314610451578063b77bf600146104bf578063ba51a6df146104e1578063c01a8c84146104f6578063c64274741461050b578063d74f8edd14610580578063dc8452cd146105a2578063e20056e6146105c4578063ee22610b146105e8575b6101515b600034111561014e57604080513481529051600160a060020a033316917fe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c919081900360200190a25b5b565b005b341561015b57fe5b6101666004356105fd565b60408051600160a060020a039092168252519081900360200190f35b341561018a57fe5b610151600160a060020a036004351661062f565b005b34156101a857fe5b6101516004356107e0565b005b34156101bd57fe5b6101d1600160a060020a03600435166108bd565b604080519115158252519081900360200190f35b34156101ed57fe5b6101d1600435600160a060020a03602435166108d2565b604080519115158252519081900360200190f35b341561022057fe5b610232600435151560243515156108f2565b60408051918252519081900360200190f35b341561024c57fe5b610151600160a060020a0360043516610961565b005b341561026a57fe5b6101d1600435610a98565b604080519115158252519081900360200190f35b341561029157fe5b610232600435610b2c565b60408051918252519081900360200190f35b34156102b657fe5b6102c1600435610bab565b60408051600160a060020a03861681526020810185905282151560608201526080918101828152845460026000196101006001841615020190911604928201839052909160a0830190859080156103595780601f1061032e57610100808354040283529160200191610359565b820191906000526020600020905b81548152906001019060200180831161033c57829003601f168201915b50509550505050505060405180910390f35b341561037357fe5b61037b610bdf565b60408051602080825283518183015283519192839290830191858101910280838382156103c3575b8051825260208311156103c357601f1990920191602091820191016103a3565b5050509050019250505060405180910390f35b34156103de57fe5b61037b60043560243560443515156064351515610c48565b60408051602080825283518183015283519192839290830191858101910280838382156103c3575b8051825260208311156103c357601f1990920191602091820191016103a3565b5050509050019250505060405180910390f35b341561045957fe5b61037b600435610d7d565b60408051602080825283518183015283519192839290830191858101910280838382156103c3575b8051825260208311156103c357601f1990920191602091820191016103a3565b5050509050019250505060405180910390f35b34156104c757fe5b610232610f05565b60408051918252519081900360200190f35b34156104e957fe5b610151600435610f0b565b005b34156104fe57fe5b610151600435610f9b565b005b341561051357fe5b604080516020600460443581810135601f8101849004840285018401909552848452610232948235600160a060020a031694602480359560649492939190920191819084018382808284375094965061108995505050505050565b60408051918252519081900360200190f35b341561058857fe5b6102326110a9565b60408051918252519081900360200190f35b34156105aa57fe5b6102326110ae565b60408051918252519081900360200190f35b34156105cc57fe5b610151600160a060020a03600435811690602435166110b4565b005b34156105f057fe5b610151600435611270565b005b600380548290811061060b57fe5b906000526020600020900160005b915054906101000a9004600160a060020a031681565b600030600160a060020a031633600160a060020a03161415156106525760006000fd5b600160a060020a038216600090815260026020526040902054829060ff16151561067c5760006000fd5b600160a060020a0383166000908152600260205260408120805460ff1916905591505b600354600019018210156107775782600160a060020a03166003838154811015156106c657fe5b906000526020600020900160005b9054906101000a9004600160a060020a0316600160a060020a0316141561076b5760038054600019810190811061070757fe5b906000526020600020900160005b9054906101000a9004600160a060020a031660038381548110151561073657fe5b906000526020600020900160005b6101000a815481600160a060020a030219169083600160a060020a03160217905550610777565b5b60019091019061069f565b60038054600019019061078a90826114cb565b5060035460045411156107a3576003546107a390610f0b565b5b604051600160a060020a038416907f8001553a916ef2f495d26a907cc54d96ed840d7bda71e73194bf5a9df7a76b9090600090a25b5b505b5050565b33600160a060020a03811660009081526002602052604090205460ff1615156108095760006000fd5b600082815260016020908152604080832033600160a060020a038116855292529091205483919060ff16151561083f5760006000fd5b600084815260208190526040902060030154849060ff16156108615760006000fd5b6000858152600160209081526040808320600160a060020a0333168085529252808320805460ff191690555187927ff6a317157440607f36269043eb55f1287a5a19ba2216afeab88cd46cbcfb88e991a35b5b505b50505b5050565b60026020526000908152604090205460ff1681565b600160209081526000928352604080842090915290825290205460ff1681565b6000805b6005548110156109595783801561091f575060008181526020819052604090206003015460ff16155b806109435750828015610943575060008181526020819052604090206003015460ff165b5b15610950576001820191505b5b6001016108f6565b5b5092915050565b30600160a060020a031633600160a060020a03161415156109825760006000fd5b600160a060020a038116600090815260026020526040902054819060ff16156109ab5760006000fd5b81600160a060020a03811615156109c25760006000fd5b60038054905060010160045460328211806109dc57508181115b806109e5575080155b806109ee575081155b156109f95760006000fd5b600160a060020a0385166000908152600260205260409020805460ff191660019081179091556003805490918101610a3183826114cb565b916000526020600020900160005b8154600160a060020a03808a166101009390930a838102910219909116179091556040519091507ff39e6e1eb0edcf53c221607b54b00cd28f3196fed0a24994dc308b8f611b682d90600090a25b5b50505b505b505b50565b600080805b600354811015610b245760008481526001602052604081206003805491929184908110610ac657fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610b08576001820191505b600454821415610b1b5760019250610b24565b5b600101610a9d565b5b5050919050565b6000805b600354811015610ba45760008381526001602052604081206003805491929184908110610b5957fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610b9b576001820191505b5b600101610b30565b5b50919050565b6000602081905290815260409020805460018201546003830154600160a060020a0390921692909160029091019060ff1684565b610be761151f565b6003805480602002602001604051908101604052809291908181526020018280548015610c3d57602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610c1f575b505050505090505b90565b610c5061151f565b610c5861151f565b60006000600554604051805910610c6c5750595b908082528060200260200182016040525b50925060009150600090505b600554811015610d0657858015610cb2575060008181526020819052604090206003015460ff16155b80610cd65750848015610cd6575060008181526020819052604090206003015460ff165b5b15610cfd57808383815181101515610ceb57fe5b60209081029091010152600191909101905b5b600101610c89565b878703604051805910610d165750595b908082528060200260200182016040525b5093508790505b86811015610d71578281815181101515610d4457fe5b9060200190602002015184898303815181101515610d5e57fe5b602090810290910101525b600101610d2e565b5b505050949350505050565b610d8561151f565b610d8d61151f565b6003546040516000918291805910610da25750595b908082528060200260200182016040525b50925060009150600090505b600354811015610e875760008581526001602052604081206003805491929184908110610de857fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610e7e576003805482908110610e3157fe5b906000526020600020900160005b9054906101000a9004600160a060020a03168383815181101515610e5f57fe5b600160a060020a03909216602092830290910190910152600191909101905b5b600101610dbf565b81604051805910610e955750595b908082528060200260200182016040525b509350600090505b81811015610efc578281815181101515610ec457fe5b906020019060200201518482815181101515610edc57fe5b600160a060020a039092166020928302909101909101525b600101610eae565b5b505050919050565b60055481565b30600160a060020a031633600160a060020a0316141515610f2c5760006000fd5b600354816032821180610f3e57508181115b80610f47575080155b80610f50575081155b15610f5b5760006000fd5b60048390556040805184815290517fa3f1ee9126a074d9326c682f561767f710e927faa811f7a99829d49dc421797a9181900360200190a15b5b50505b50565b33600160a060020a03811660009081526002602052604090205460ff161515610fc45760006000fd5b6000828152602081905260409020548290600160a060020a03161515610fea5760006000fd5b600083815260016020908152604080832033600160a060020a038116855292529091205484919060ff161561101f5760006000fd5b6000858152600160208181526040808420600160a060020a0333168086529252808420805460ff1916909317909255905187927f4a504a94899432a9846e1aa406dceb1bcfd538bb839071d49d1e5e23f5be30ef91a36108b385611270565b5b5b50505b505b5050565b60006110968484846113d8565b90506110a181610f9b565b5b9392505050565b603281565b60045481565b600030600160a060020a031633600160a060020a03161415156110d75760006000fd5b600160a060020a038316600090815260026020526040902054839060ff1615156111015760006000fd5b600160a060020a038316600090815260026020526040902054839060ff161561112a5760006000fd5b600092505b6003548310156111d25784600160a060020a031660038481548110151561115257fe5b906000526020600020900160005b9054906101000a9004600160a060020a0316600160a060020a031614156111c6578360038481548110151561119157fe5b906000526020600020900160005b6101000a815481600160a060020a030219169083600160a060020a031602179055506111d2565b5b60019092019161112f565b600160a060020a03808616600081815260026020526040808220805460ff1990811690915593881682528082208054909416600117909355915190917f8001553a916ef2f495d26a907cc54d96ed840d7bda71e73194bf5a9df7a76b9091a2604051600160a060020a038516907ff39e6e1eb0edcf53c221607b54b00cd28f3196fed0a24994dc308b8f611b682d90600090a25b5b505b505b505050565b600081815260208190526040812060030154829060ff16156112925760006000fd5b61129b83610a98565b156107d9576000838152602081905260409081902060038101805460ff19166001908117909155815481830154935160028085018054959850600160a060020a03909316959492939192839285926000199183161561010002919091019091160480156113495780601f1061131e57610100808354040283529160200191611349565b820191906000526020600020905b81548152906001019060200180831161132c57829003601f168201915b505091505060006040518083038185876185025a03f192505050156113985760405183907f33e13ecb54c3076d8e8bb8c2881800a4d972b792045ffae98fdf46df365fed7590600090a26107d9565b60405183907f526441bb6c1aba3c9a4a6ca1d6545da9c2333c8c48343ef398eb858d72b7923690600090a260038201805460ff191690555b5b5b5b505050565b600083600160a060020a03811615156113f15760006000fd5b60055460408051608081018252600160a060020a0388811682526020808301898152838501898152600060608601819052878152808452959095208451815473ffffffffffffffffffffffffffffffffffffffff191694169390931783555160018301559251805194965091939092611471926002850192910190611543565b50606091909101516003909101805460ff191691151591909117905560058054600101905560405182907fc0ba8fe4b176c1714197d43b9cc6bcf797a4a7461c5fe8d0ef6e184ae7601e5190600090a25b5b509392505050565b8154818355818115116107d9576000838152602090206107d99181019083016115c2565b5b505050565b8154818355818115116107d9576000838152602090206107d99181019083016115c2565b5b505050565b60408051602081019091526000815290565b60408051602081019091526000815290565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061158457805160ff19168380011785556115b1565b828001600101855582156115b1579182015b828111156115b1578251825591602001919060010190611596565b5b506115be9291506115c2565b5090565b610c4591905b808211156115be57600081556001016115c8565b5090565b905600a165627a7a723058202ec03a2008a824a2f91b32b903984a34feac5d9483d91d1ff068490dc12bcd120029";

    public static final String FUNC_OWNERS = "owners";

    public static final String FUNC_REMOVEOWNER = "removeOwner";

    public static final String FUNC_REVOKECONFIRMATION = "revokeConfirmation";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_CONFIRMATIONS = "confirmations";

    public static final String FUNC_GETTRANSACTIONCOUNT = "getTransactionCount";

    public static final String FUNC_ADDOWNER = "addOwner";

    public static final String FUNC_ISCONFIRMED = "isConfirmed";

    public static final String FUNC_GETCONFIRMATIONCOUNT = "getConfirmationCount";

    public static final String FUNC_TRANSACTIONS = "transactions";

    public static final String FUNC_GETOWNERS = "getOwners";

    public static final String FUNC_GETTRANSACTIONIDS = "getTransactionIds";

    public static final String FUNC_GETCONFIRMATIONS = "getConfirmations";

    public static final String FUNC_TRANSACTIONCOUNT = "transactionCount";

    public static final String FUNC_CHANGEREQUIREMENT = "changeRequirement";

    public static final String FUNC_CONFIRMTRANSACTION = "confirmTransaction";

    public static final String FUNC_SUBMITTRANSACTION = "submitTransaction";

    public static final String FUNC_MAX_OWNER_COUNT = "MAX_OWNER_COUNT";

    public static final String FUNC_REQUIRED = "required";

    public static final String FUNC_REPLACEOWNER = "replaceOwner";

    public static final String FUNC_EXECUTETRANSACTION = "executeTransaction";

    public static final Event CONFIRMATION_EVENT = new Event("Confirmation",
        Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event REVOCATION_EVENT = new Event("Revocation",
        Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event SUBMISSION_EVENT = new Event("Submission",
        Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event EXECUTION_EVENT = new Event("Execution",
        Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event EXECUTIONFAILURE_EVENT = new Event("ExecutionFailure",
        Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event DEPOSIT_EVENT = new Event("Deposit",
        Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERADDITION_EVENT = new Event("OwnerAddition",
        Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERREMOVAL_EVENT = new Event("OwnerRemoval",
        Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event REQUIREMENTCHANGE_EVENT = new Event("RequirementChange",
        Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected MultiSigWallet(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public MultiSigWallet(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MultiSigWallet(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public MultiSigWallet(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<String> owners(BigInteger param0) {
        final Function function = new Function(FUNC_OWNERS,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> removeOwner(String owner) {
        final Function function = new Function(
            FUNC_REMOVEOWNER,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeConfirmation(BigInteger transactionId) {
        final Function function = new Function(
            FUNC_REVOKECONFIRMATION,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isOwner(String param0) {
        final Function function = new Function(FUNC_ISOWNER,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> confirmations(BigInteger param0, String param1) {
        final Function function = new Function(FUNC_CONFIRMATIONS,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0),
                new org.web3j.abi.datatypes.Address(160, param1)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> getTransactionCount(Boolean pending, Boolean executed) {
        final Function function = new Function(FUNC_GETTRANSACTIONCOUNT,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Bool(pending),
                new org.web3j.abi.datatypes.Bool(executed)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> addOwner(String owner) {
        final Function function = new Function(
            FUNC_ADDOWNER,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isConfirmed(BigInteger transactionId) {
        final Function function = new Function(FUNC_ISCONFIRMED,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> getConfirmationCount(BigInteger transactionId) {
        final Function function = new Function(FUNC_GETCONFIRMATIONCOUNT,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple4<String, BigInteger, byte[], Boolean>> transactions(BigInteger param0) {
        final Function function = new Function(FUNC_TRANSACTIONS,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple4<String, BigInteger, byte[], Boolean>>(function,
            new Callable<Tuple4<String, BigInteger, byte[], Boolean>>() {
                @Override
                public Tuple4<String, BigInteger, byte[], Boolean> call() throws Exception {
                    List<Type> results = executeCallMultipleValueReturn(function);
                    return new Tuple4<String, BigInteger, byte[], Boolean>(
                        (String) results.get(0).getValue(),
                        (BigInteger) results.get(1).getValue(),
                        (byte[]) results.get(2).getValue(),
                        (Boolean) results.get(3).getValue());
                }
            });
    }

    public RemoteFunctionCall<List> getOwners() {
        final Function function = new Function(FUNC_GETOWNERS,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
            new Callable<List>() {
                @Override
                @SuppressWarnings("unchecked")
                public List call() throws Exception {
                    List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                    return convertToNative(result);
                }
            });
    }

    public RemoteFunctionCall<List> getTransactionIds(BigInteger from, BigInteger to, Boolean pending, Boolean executed) {
        final Function function = new Function(FUNC_GETTRANSACTIONIDS,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(from),
                new org.web3j.abi.datatypes.generated.Uint256(to),
                new org.web3j.abi.datatypes.Bool(pending),
                new org.web3j.abi.datatypes.Bool(executed)),
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
            new Callable<List>() {
                @Override
                @SuppressWarnings("unchecked")
                public List call() throws Exception {
                    List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                    return convertToNative(result);
                }
            });
    }

    public RemoteFunctionCall<List> getConfirmations(BigInteger transactionId) {
        final Function function = new Function(FUNC_GETCONFIRMATIONS,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
            new Callable<List>() {
                @Override
                @SuppressWarnings("unchecked")
                public List call() throws Exception {
                    List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                    return convertToNative(result);
                }
            });
    }

    public RemoteFunctionCall<BigInteger> transactionCount() {
        final Function function = new Function(FUNC_TRANSACTIONCOUNT,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> changeRequirement(BigInteger _required) {
        final Function function = new Function(
            FUNC_CHANGEREQUIREMENT,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_required)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> confirmTransaction(BigInteger transactionId) {
        final Function function = new Function(
            FUNC_CONFIRMTRANSACTION,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> submitTransaction(String destination, BigInteger value, byte[] data) {
        final Function function = new Function(
            FUNC_SUBMITTRANSACTION,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, destination),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.DynamicBytes(data)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> MAX_OWNER_COUNT() {
        final Function function = new Function(FUNC_MAX_OWNER_COUNT,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> required() {
        final Function function = new Function(FUNC_REQUIRED,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> replaceOwner(String owner, String newOwner) {
        final Function function = new Function(
            FUNC_REPLACEOWNER,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                new org.web3j.abi.datatypes.Address(160, newOwner)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> executeTransaction(BigInteger transactionId) {
        final Function function = new Function(
            FUNC_EXECUTETRANSACTION,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(transactionId)),
            Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<ConfirmationEventResponse> getConfirmationEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CONFIRMATION_EVENT, transactionReceipt);
        ArrayList<ConfirmationEventResponse> responses = new ArrayList<ConfirmationEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ConfirmationEventResponse typedResponse = new ConfirmationEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ConfirmationEventResponse> confirmationEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ConfirmationEventResponse>() {
            @Override
            public ConfirmationEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CONFIRMATION_EVENT, log);
                ConfirmationEventResponse typedResponse = new ConfirmationEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ConfirmationEventResponse> confirmationEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CONFIRMATION_EVENT));
        return confirmationEventFlowable(filter);
    }

    public List<RevocationEventResponse> getRevocationEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REVOCATION_EVENT, transactionReceipt);
        ArrayList<RevocationEventResponse> responses = new ArrayList<RevocationEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RevocationEventResponse typedResponse = new RevocationEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RevocationEventResponse> revocationEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, RevocationEventResponse>() {
            @Override
            public RevocationEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REVOCATION_EVENT, log);
                RevocationEventResponse typedResponse = new RevocationEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RevocationEventResponse> revocationEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REVOCATION_EVENT));
        return revocationEventFlowable(filter);
    }

    public List<SubmissionEventResponse> getSubmissionEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SUBMISSION_EVENT, transactionReceipt);
        ArrayList<SubmissionEventResponse> responses = new ArrayList<SubmissionEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SubmissionEventResponse typedResponse = new SubmissionEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SubmissionEventResponse> submissionEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, SubmissionEventResponse>() {
            @Override
            public SubmissionEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SUBMISSION_EVENT, log);
                SubmissionEventResponse typedResponse = new SubmissionEventResponse();
                typedResponse.log = log;
                typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SubmissionEventResponse> submissionEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SUBMISSION_EVENT));
        return submissionEventFlowable(filter);
    }

    public List<ExecutionEventResponse> getExecutionEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTION_EVENT, transactionReceipt);
        ArrayList<ExecutionEventResponse> responses = new ArrayList<ExecutionEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionEventResponse typedResponse = new ExecutionEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionEventResponse> executionEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ExecutionEventResponse>() {
            @Override
            public ExecutionEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTION_EVENT, log);
                ExecutionEventResponse typedResponse = new ExecutionEventResponse();
                typedResponse.log = log;
                typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionEventResponse> executionEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTION_EVENT));
        return executionEventFlowable(filter);
    }

    public List<ExecutionFailureEventResponse> getExecutionFailureEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXECUTIONFAILURE_EVENT, transactionReceipt);
        ArrayList<ExecutionFailureEventResponse> responses = new ArrayList<ExecutionFailureEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExecutionFailureEventResponse> executionFailureEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ExecutionFailureEventResponse>() {
            @Override
            public ExecutionFailureEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXECUTIONFAILURE_EVENT, log);
                ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
                typedResponse.log = log;
                typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExecutionFailureEventResponse> executionFailureEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXECUTIONFAILURE_EVENT));
        return executionFailureEventFlowable(filter);
    }

    public List<DepositEventResponse> getDepositEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEPOSIT_EVENT, transactionReceipt);
        ArrayList<DepositEventResponse> responses = new ArrayList<DepositEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DepositEventResponse typedResponse = new DepositEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DepositEventResponse> depositEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, DepositEventResponse>() {
            @Override
            public DepositEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEPOSIT_EVENT, log);
                DepositEventResponse typedResponse = new DepositEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DepositEventResponse> depositEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEPOSIT_EVENT));
        return depositEventFlowable(filter);
    }

    public List<OwnerAdditionEventResponse> getOwnerAdditionEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERADDITION_EVENT, transactionReceipt);
        ArrayList<OwnerAdditionEventResponse> responses = new ArrayList<OwnerAdditionEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerAdditionEventResponse typedResponse = new OwnerAdditionEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnerAdditionEventResponse> ownerAdditionEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, OwnerAdditionEventResponse>() {
            @Override
            public OwnerAdditionEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERADDITION_EVENT, log);
                OwnerAdditionEventResponse typedResponse = new OwnerAdditionEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnerAdditionEventResponse> ownerAdditionEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERADDITION_EVENT));
        return ownerAdditionEventFlowable(filter);
    }

    public List<OwnerRemovalEventResponse> getOwnerRemovalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERREMOVAL_EVENT, transactionReceipt);
        ArrayList<OwnerRemovalEventResponse> responses = new ArrayList<OwnerRemovalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerRemovalEventResponse typedResponse = new OwnerRemovalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnerRemovalEventResponse> ownerRemovalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, OwnerRemovalEventResponse>() {
            @Override
            public OwnerRemovalEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERREMOVAL_EVENT, log);
                OwnerRemovalEventResponse typedResponse = new OwnerRemovalEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnerRemovalEventResponse> ownerRemovalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERREMOVAL_EVENT));
        return ownerRemovalEventFlowable(filter);
    }

    public List<RequirementChangeEventResponse> getRequirementChangeEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REQUIREMENTCHANGE_EVENT, transactionReceipt);
        ArrayList<RequirementChangeEventResponse> responses = new ArrayList<RequirementChangeEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RequirementChangeEventResponse typedResponse = new RequirementChangeEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.required = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RequirementChangeEventResponse> requirementChangeEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, RequirementChangeEventResponse>() {
            @Override
            public RequirementChangeEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REQUIREMENTCHANGE_EVENT, log);
                RequirementChangeEventResponse typedResponse = new RequirementChangeEventResponse();
                typedResponse.log = log;
                typedResponse.required = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RequirementChangeEventResponse> requirementChangeEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REQUIREMENTCHANGE_EVENT));
        return requirementChangeEventFlowable(filter);
    }

    @Deprecated
    public static MultiSigWallet load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiSigWallet(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MultiSigWallet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiSigWallet(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MultiSigWallet load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MultiSigWallet(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MultiSigWallet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MultiSigWallet(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MultiSigWallet> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, List<String> _owners, BigInteger _required) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                org.web3j.abi.datatypes.Address.class,
                org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)),
            new org.web3j.abi.datatypes.generated.Uint256(_required)));
        return deployRemoteCall(MultiSigWallet.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<MultiSigWallet> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, List<String> _owners, BigInteger _required) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                org.web3j.abi.datatypes.Address.class,
                org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)),
            new org.web3j.abi.datatypes.generated.Uint256(_required)));
        return deployRemoteCall(MultiSigWallet.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MultiSigWallet> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, List<String> _owners, BigInteger _required) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                org.web3j.abi.datatypes.Address.class,
                org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)),
            new org.web3j.abi.datatypes.generated.Uint256(_required)));
        return deployRemoteCall(MultiSigWallet.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MultiSigWallet> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, List<String> _owners, BigInteger _required) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                org.web3j.abi.datatypes.Address.class,
                org.web3j.abi.Utils.typeMap(_owners, org.web3j.abi.datatypes.Address.class)),
            new org.web3j.abi.datatypes.generated.Uint256(_required)));
        return deployRemoteCall(MultiSigWallet.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class ConfirmationEventResponse extends BaseEventResponse {
        public String sender;

        public BigInteger transactionId;
    }

    public static class RevocationEventResponse extends BaseEventResponse {
        public String sender;

        public BigInteger transactionId;
    }

    public static class SubmissionEventResponse extends BaseEventResponse {
        public BigInteger transactionId;
    }

    public static class ExecutionEventResponse extends BaseEventResponse {
        public BigInteger transactionId;
    }

    public static class ExecutionFailureEventResponse extends BaseEventResponse {
        public BigInteger transactionId;
    }

    public static class DepositEventResponse extends BaseEventResponse {
        public String sender;

        public BigInteger value;
    }

    public static class OwnerAdditionEventResponse extends BaseEventResponse {
        public String owner;
    }

    public static class OwnerRemovalEventResponse extends BaseEventResponse {
        public String owner;
    }

    public static class RequirementChangeEventResponse extends BaseEventResponse {
        public BigInteger required;
    }
}
