package com.alaphi.accountservice.repository

import com.alaphi.accountservice.model.Account._

trait AccountRepository[F[_]] {
  def create(accountCreation: AccountCreation): F[Account]
  def read(accountNumber: String): F[Either[AccountError, Account]]
  def readAll: F[Seq[Account]]
  def deposit(accountNumber: String, amount: Int): F[Either[AccountError, DepositSuccess]]
  def transfer(srcAccNum: String, destAccNum: String, amount: Int): F[Either[AccountError, TransferSuccess]]
}
