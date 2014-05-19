package io.bitsquare.gui.util;

import com.google.bitcoin.core.*;
import com.google.bitcoin.script.Script;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConfidenceDisplay
{
    private static final Logger log = LoggerFactory.getLogger(ConfidenceDisplay.class);
    private WalletEventListener walletEventListener;

    private Wallet wallet;
    private Label confirmationLabel;
    private TextField balanceTextField;
    private Transaction transaction;
    private ProgressIndicator progressIndicator;

    /**
     * We got the confidence for the actual updating tx.
     *
     * @param wallet
     * @param confirmationLabel
     * @param balanceTextField
     * @param progressIndicator
     */
    public ConfidenceDisplay(Wallet wallet, Label confirmationLabel, TextField balanceTextField, ProgressIndicator progressIndicator)
    {
        this.wallet = wallet;
        this.confirmationLabel = confirmationLabel;
        this.balanceTextField = balanceTextField;
        this.progressIndicator = progressIndicator;

        balanceTextField.setText("");
        confirmationLabel.setVisible(false);
        progressIndicator.setVisible(false);
        progressIndicator.setProgress(0);

        updateBalance(wallet.getBalance());
        walletEventListener = new WalletEventListener()
        {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance)
            {
                updateBalance(newBalance);
                // log.debug("onCoinsReceived  " + newBalance);
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx)
            {
                updateConfidence(tx);
                // log.debug("onTransactionConfidenceChanged tx " + tx.getHashAsString());
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance)
            {
                updateBalance(newBalance);
            }

            @Override
            public void onReorganize(Wallet wallet)
            {
            }

            @Override
            public void onWalletChanged(Wallet wallet)
            {
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> keys)
            {
            }

            @Override
            public void onScriptsAdded(Wallet wallet, List<Script> scripts)
            {
            }
        };
        wallet.addEventListener(walletEventListener);
    }

    /**
     * @param wallet
     * @param confirmationLabel
     * @param transaction       We want the confidence for only that tx, not the lasted changed in the wallet
     * @param progressIndicator
     */
    public ConfidenceDisplay(Wallet wallet, Label confirmationLabel, final Transaction transaction, ProgressIndicator progressIndicator)
    {
        this.wallet = wallet;
        this.confirmationLabel = confirmationLabel;
        this.transaction = transaction;
        this.progressIndicator = progressIndicator;

        if (balanceTextField != null)
            balanceTextField.setText("");
        confirmationLabel.setVisible(false);
        progressIndicator.setVisible(false);
        progressIndicator.setProgress(0);

        updateBalance(wallet.getBalance());
        updateConfidence(transaction);

        walletEventListener = new WalletEventListener()
        {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateBalance(newBalance);
                // log.debug("onCoinsReceived " + newBalance);
            }

            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateConfidence(transaction);
                // log.debug("onTransactionConfidenceChanged newTransaction " + newTransaction.getHashAsString());
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance)
            {
                if (tx.getHashAsString().equals(transaction.getHashAsString()))
                    updateBalance(newBalance);
            }

            @Override
            public void onReorganize(Wallet wallet)
            {
            }

            @Override
            public void onWalletChanged(Wallet wallet)
            {
            }

            @Override
            public void onKeysAdded(Wallet wallet, List<ECKey> keys)
            {
            }

            @Override
            public void onScriptsAdded(Wallet wallet, List<Script> scripts)
            {
            }
        };
        wallet.addEventListener(walletEventListener);
    }

    public void destroy()
    {
        boolean wasInList = wallet.removeEventListener(walletEventListener);
        log.trace("ConfidenceDisplay.destroy wasInList = " + wasInList);
        progressIndicator.setProgress(0);
        confirmationLabel.setText("");
        if (balanceTextField != null)
            balanceTextField.setText("");

        walletEventListener = null;
        wallet = null;
        confirmationLabel = null;
        progressIndicator = null;
        balanceTextField = null;
    }

    private void updateBalance(BigInteger balance)
    {
        if (balance.compareTo(BigInteger.ZERO) > 0)
        {
            confirmationLabel.setVisible(true);
            progressIndicator.setVisible(true);
            progressIndicator.setProgress(-1);

            Set<Transaction> transactions = wallet.getTransactions(false);
            Transaction latestTransaction = null;
            for (Iterator<Transaction> iterator = transactions.iterator(); iterator.hasNext(); )
            {
                Transaction transaction = iterator.next();
                if (latestTransaction != null)
                {
                    if (transaction.getUpdateTime().compareTo(latestTransaction.getUpdateTime()) > 0)
                    {
                        latestTransaction = transaction;
                    }
                }
                else
                {
                    latestTransaction = transaction;
                }
            }
            if (latestTransaction != null && (transaction == null || latestTransaction.getHashAsString().equals(transaction.getHashAsString())))
                updateConfidence(latestTransaction);
        }

        if (balanceTextField != null)
            balanceTextField.setText(Utils.bitcoinValueToFriendlyString(balance));
    }

    private void updateConfidence(Transaction tx)
    {
        log.debug("updateConfidence: " + this.toString());
        log.debug("tx: " + tx.getHashAsString());
        TransactionConfidence confidence = tx.getConfidence();
        log.debug("ConfidenceType: " + confidence.getConfidenceType().toString());
        log.debug("numBroadcastPeers: " + confidence.numBroadcastPeers());
        log.debug("getDepthInBlocks: " + confidence.getDepthInBlocks());
        double progressIndicatorSize = 50;
        switch (confidence.getConfidenceType())
        {
            case UNKNOWN:
                confirmationLabel.setText("");
                progressIndicator.setProgress(0);
                break;
            case PENDING:
                confirmationLabel.setText("Seen by " + confidence.numBroadcastPeers() + " peer(s) / 0 confirmations");
                progressIndicator.setProgress(-1.0);
                progressIndicatorSize = 20;
                break;
            case BUILDING:
                confirmationLabel.setText("Confirmed in " + confidence.getDepthInBlocks() + " block(s)");
                progressIndicator.setProgress(Math.min(1, (double) confidence.getDepthInBlocks() / 6.0));
                break;
            case DEAD:
                confirmationLabel.setText("Transaction is invalid.");
                break;
        }

        progressIndicator.setMaxHeight(progressIndicatorSize);
        progressIndicator.setPrefHeight(progressIndicatorSize);
        progressIndicator.setMaxWidth(progressIndicatorSize);
        progressIndicator.setPrefWidth(progressIndicatorSize);
    }


}
