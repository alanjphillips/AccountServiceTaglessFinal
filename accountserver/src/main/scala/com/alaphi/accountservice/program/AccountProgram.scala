package com.alaphi.accountservice.program

import com.alaphi.accountservice.model.Account._
import com.alaphi.accountservice.repository.AccountRepository

class AccountProgram[F[_]](accountRepository: AccountRepository[F]) extends AccountAlgebra[F] {

  def create(accountCreation: AccountCreation): F[Account] =
    accountRepository.create(accountCreation)

  def read(accountNumber: String): F[Either[AccountError, Account]] =
    accountRepository.read(accountNumber)

  def readAll: F[Seq[Account]] =
    accountRepository.readAll

  def deposit(accountNumber: String, deposit: Deposit): F[Either[AccountError, DepositSuccess]] =
    accountRepository.deposit(accountNumber, deposit.depositAmount)

  def transfer(accountNumber: String, transfer: MoneyTransfer): F[Either[AccountError, TransferSuccess]] =
    accountRepository.transfer(accountNumber, transfer.destAccNum, transfer.transferAmount)

}
