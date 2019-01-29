package com.alaphi.accountservice.program

import com.alaphi.accountservice.model.Account._

trait AccountAlgebra[F[_]] {
  def create(accountCreation: AccountCreation): F[Account]
  def read(accountNumber: String): F[Either[AccountError, Account]]
  def readAll: F[Seq[Account]]
  def deposit(accountNumber: String, deposit: Deposit): F[Either[AccountError, DepositSuccess]]
  def transfer(accountNumber: String, transfer: MoneyTransfer): F[Either[AccountError, TransferSuccess]]
}
