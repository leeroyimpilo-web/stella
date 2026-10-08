import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, randomBytes } from "node:crypto";
import {
  generateSerial, serialHash, encryptSerial, decryptSerial, PRICE_CENTS,
  payFastSignature, payFastParamString, verifyPaymentData,
  signOfflineLicense, verifyOfflineLicense
} from "../lib.mjs";

test("fixed product price and unique serials", () => {
  assert.equal(PRICE_CENTS, 5000);
  const a=generateSerial(), b=generateSerial();
  assert.match(a, /^LTI-([A-F0-9]{6}-){3}[A-F0-9]{6}$/);
  assert.notEqual(a,b);
  assert.equal(serialHash(a, "a".repeat(64)), serialHash(a.toLowerCase(), "a".repeat(64)));
});
test("serial encryption roundtrip and wrong key rejected", () => {
  const k=randomBytes(32).toString("base64"), s=generateSerial();
  const encrypted=encryptSerial(s,k);
  assert.notEqual(encrypted,s);
  assert.equal(decryptSerial(encrypted,k),s);
  assert.throws(()=>decryptSerial(encrypted, randomBytes(32).toString("base64")));
});
test("payment signature excludes split setup", () => {
  const fields=[["merchant_id","10000100"],["amount","50.00"],["setup","{\"split_payment\":{}}"]];
  assert.equal(payFastSignature(fields,"secret"),payFastSignature(fields.slice(0,2),"secret"));
  assert.ok(payFastParamString([["name_first","Ms Nandi"]],"pass").includes("Ms+Nandi"));
});
test("payment amount, status and merchant must match", () => {
  const order={id:"order-1",amount_cents:5000};
  const valid={payment_status:"COMPLETE",m_payment_id:"order-1",amount_gross:"50.00",merchant_id:"10000100",pf_payment_id:"31415"};
  assert.equal(verifyPaymentData(valid,order,"10000100"),true);
  assert.equal(verifyPaymentData({...valid,amount_gross:"5.00"},order,"10000100"),false);
  assert.equal(verifyPaymentData({...valid,payment_status:"CANCELLED"},order,"10000100"),false);
  assert.equal(verifyPaymentData({...valid,merchant_id:"other"},order,"10000100"),false);
});
test("offline licence is bound to device and installation", () => {
  const {publicKey,privateKey}=generateKeyPairSync("rsa",{modulusLength:2048});
  const body={product:"lotto-intelligence",deviceHash:"a".repeat(64),installationId:"00000000-0000-4000-8000-000000000001"};
  const token=signOfflineLicense(body,privateKey);
  assert.equal(verifyOfflineLicense(token,publicKey,body.deviceHash,body.installationId),true);
  assert.equal(verifyOfflineLicense(token,publicKey,"b".repeat(64),body.installationId),false);
  assert.equal(verifyOfflineLicense(token,publicKey,body.deviceHash,"00000000-0000-4000-8000-000000000002"),false);
  assert.equal(verifyOfflineLicense(token+"x",publicKey,body.deviceHash,body.installationId),false);
});
