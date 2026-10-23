package com.aminul.multibook.data.repository

import com.aminul.multibook.data.dao.AccountDao
import com.aminul.multibook.data.model.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AccountRepository(private val accountDao: AccountDao) {

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun getAccount(id: String): AccountEntity? = accountDao.getAccountById(id)

    suspend fun addAccount(name: String, colorHex: String): AccountEntity {
        val count = accountDao.getAccountCount()
        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex,
            orderIndex = count,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis()
        )
        accountDao.insertAccount(account)
        return account
    }

    suspend fun updateName(id: String, newName: String) {
        val existing = accountDao.getAccountById(id) ?: return
        accountDao.updateAccount(existing.copy(name = newName.trim()))
    }

    suspend fun updateColor(id: String, newColorHex: String) {
        val existing = accountDao.getAccountById(id) ?: return
        accountDao.updateAccount(existing.copy(colorHex = newColorHex))
    }

    suspend fun touchAccount(id: String) {
        val existing = accountDao.getAccountById(id) ?: return
        accountDao.updateAccount(existing.copy(lastUsedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(id: String) {
        accountDao.deleteAccountById(id)
    }

    suspend fun getAccountCount(): Int = accountDao.getAccountCount()
}
