package rocks.crownstone.bluenet

import org.junit.Assert
import org.junit.Test
import rocks.crownstone.bluenet.structs.Errors

class PlaygroundTest {
	@Test
	fun test() {
		val err: Exception = Errors.EncryptionKeyMissing()
		println("err type is ${err.javaClass}")

		if (err is Errors.EncryptionKeyMissing) {
			println("err is of type Errors.EncryptionKeyMissing")
		}
		if (err is Errors.Encryption) {
			println("err is of parent type Errors.Encryption")
		}
		if (err is Errors.Timeout) {
			println("err is of type Errors.Timeout")
		}
		else {
			println("err is not of type Errors.Timeout")
		}
//		print("encryption error type is ${Errors.Encryption}")
		Assert.assertTrue(err is Errors.Encryption)
		Assert.assertFalse(err is Errors.Timeout)
	}
}