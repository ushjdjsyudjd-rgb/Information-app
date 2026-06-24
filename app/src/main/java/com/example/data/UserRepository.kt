package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    fun searchUsers(query: String): Flow<List<UserEntity>> {
        val formattedQuery = "%$query%"
        return userDao.searchUsers(formattedQuery)
    }

    suspend fun insert(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    suspend fun update(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun delete(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun deleteById(userId: Int) {
        userDao.deleteUserById(userId)
    }
}
