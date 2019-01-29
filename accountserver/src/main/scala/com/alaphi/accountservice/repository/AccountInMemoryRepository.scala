package com.alaphi.accountservice.repository

import com.alaphi.accountservice.db.AccountInMemoryDatabase
import com.alaphi.accountservice.model.Account._

class AccountInMemoryRepository[F[_]](db: AccountInMemoryDatabase[F]) extends AccountRepository[F] {

  def create(accountCreation: AccountCreation): F[Account] =
    db.create(accountCreation)

  def read(accountNumber: String): F[Either[AccountError, Account]] =
    db.read(accountNumber).value

  def readAll: F[Seq[Account]] = db.readAll

  def deposit(accountNumber: String, amount: Int): F[Either[AccountError, DepositSuccess]] =
    db.deposit(accountNumber, amount).value

  def transfer(srcAccNum: String, destAccNum: String, amount: Int): F[Either[AccountError, TransferSuccess]] =
    db.transfer(srcAccNum, destAccNum, amount).value

}
