package com.alaphi.accountservice.db

import cats.data.EitherT
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.{Ref, Semaphore}
import com.alaphi.accountservice.model.Account._

class AccountInMemoryDatabase[F[_]: ConcurrentEffect] private(storage: Ref[F, Map[String, AccountAccess[F]]])(implicit F: Sync[F], ctx: ContextShift[F]) {

  def create(accountCreation: AccountCreation): F[Account] = for {
    accountNumber <- generateAccountNumber
    accountAccess <- AccountAccess.create(Account(accountNumber, accountCreation.accHolderName, accountCreation.balance))
    account = accountAccess.account
    _ <- storage.update(_.updated(account.accNumber, accountAccess))
  } yield account

  def read(accountNumber: String): EitherT[F, AccountError, Account] =
    getAccountAccess(accountNumber).map(_.account)

  def readAll: F[Seq[Account]] =
    storage.get.map(_.values.map(_.account).toSeq)

  def deposit(accountNumber: String, amount: Int): EitherT[F, AccountError, DepositSuccess] = for {
    accAccess <- getAccountAccess(accountNumber)
    depositResult <-
      EitherT[F, AccountError, DepositSuccess](
        F.bracket(acquire(accAccess).void)(_ => persistDeposit(accAccess, amount))(_ => release(accAccess).void)
      )
  } yield depositResult

  def transfer(srcAccNum: String, destAccNum: String, amount: Int): EitherT[F, AccountError, TransferSuccess] = for {
    accAccessSrc <- getAccountAccess(srcAccNum)
    accAccessDest <- getAccountAccess(destAccNum)
    transferResult <- EitherT(
      F.bracket(acquire(accAccessSrc, accAccessDest))(_ => persistTransfer(accAccessSrc, accAccessDest, amount))(_ => release(accAccessSrc, accAccessDest).void)
    )
  } yield transferResult

  private def persistDeposit(accAccess: AccountAccess[F], amount: Int): F[Either[AccountError, DepositSuccess]] = {
    val accountDeposit = plusBalance(accAccess.account, amount)
    updateStorage(accAccess, accountDeposit)
      .map(_ => Right(DepositSuccess(accountDeposit, amount)))
  }

  private def persistTransfer(accAccessSrc: AccountAccess[F], accAccessDest: AccountAccess[F], amount: Int): F[Either[AccountError, TransferSuccess]] =
    if (accAccessSrc.account.balance >= amount) {
      val accDebit = minusBalance(accAccessSrc.account, amount)
      val accCredit = plusBalance(accAccessDest.account, amount)
      for {
        _ <- updateStorage(accAccessSrc, accDebit)
        _ <- updateStorage(accAccessDest, accCredit)
      } yield Right(TransferSuccess(accDebit, accCredit, amount))
    } else
      F.delay(Left(TransferFailed(accAccessSrc.account, accAccessDest.account, amount, s"Not enough funds available in account number: ${accAccessSrc.account.accNumber}")))

  private def acquire(accAccess: AccountAccess[F]*): F[List[Account]] =
    accAccess.toList.map(_.acquireAccount).sequence

  private def release(accAccess: AccountAccess[F]*): F[List[Account]] =
    accAccess.toList.map(_.releaseAccount).sequence

  private def getAccountAccess(accountNumber: String): EitherT[F, AccountError, AccountAccess[F]] =
    EitherT(
      storage.get
        .map(_.get(accountNumber)
          .toRight[AccountError](AccountNotFound(accountNumber, s"Account Number doesn't exist: $accountNumber")))
    )

  private def updateStorage(accAccess: AccountAccess[F], updatedAccount: Account): F[Unit] =
    storage.update(accAccMap =>
      accAccMap.updated(
        accAccess.account.accNumber,
        accAccess.copy(account = updatedAccount)
      )
    )

  private def generateAccountNumber: F[String] =
    storage.get.map(accounts => (accounts.size + 1).toString)

  private def plusBalance(account: Account, plusAmount: Int) =
    account.copy(balance = account.balance + plusAmount)

  private def minusBalance(account: Account, minusAmount: Int) =
    account.copy(balance = account.balance - minusAmount)
}

object AccountInMemoryDatabase {
  def createDB[F[_]: ConcurrentEffect](implicit F: Sync[F], ctx: ContextShift[F]): F[AccountInMemoryDatabase[F]] =
    Ref.of[F, Map[String, AccountAccess[F]]](Map.empty[String, AccountAccess[F]])
      .map(new AccountInMemoryDatabase[F](_))
}

case class AccountAccess[F[_]: ConcurrentEffect](account: Account, lock: Semaphore[F]) {
  def acquireAccount: F[Account] = lock.acquire.map(_ => account)
  def releaseAccount: F[Account] = lock.release.map(_ => account)
  def isAvailable: F[Boolean] = lock.available.map(_ > 0)
}

object AccountAccess {
  def create[F[_]: ConcurrentEffect](account: Account): F[AccountAccess[F]] =
    Semaphore[F](1).map(lock => new AccountAccess[F](account, lock))
}




