package com.alaphi.accountservice.http

import cats.effect.{IO, Sync}
import com.alaphi.accountservice.model.Account._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import io.circe.generic.auto._

object JsonCodec {
  implicit def decoderAccCreate[F[_]: Sync] = jsonOf[F, AccountCreation]
  implicit def decoderDeposit[F[_]: Sync] = jsonOf[F, Deposit]
  implicit def decoderMoneyTransfer[F[_]: Sync] = jsonOf[F, MoneyTransfer]

  implicit def encoderAcc[F[_]: Sync] = jsonEncoderOf[F, Account]
  implicit def encoderAccs[F[_]: Sync] = jsonEncoderOf[F, Seq[Account]]
  implicit def encoderDepositSuccess[F[_]: Sync] = jsonEncoderOf[F, DepositSuccess]
  implicit def encoderTransferSuccess[F[_]: Sync] = jsonEncoderOf[F, TransferSuccess]
}
