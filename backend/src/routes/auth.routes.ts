import { Router, Request, Response } from 'express'

const router = Router()

router.post('/login', async (req: Request, res: Response) => {
  try {
    const { username, password } = req.body
    // TODO: 实现登录逻辑
    res.json({
      status: 'success',
      message: '登录成功',
      data: {
        token: 'mock-jwt-token',
        user: {
          id: '1',
          username,
          email: `${username}@example.com`
        }
      }
    })
  } catch (error) {
    res.status(500).json({
      status: 'error',
      message: '登录失败'
    })
  }
})

router.post('/register', async (req: Request, res: Response) => {
  try {
    const { username, email, password } = req.body
    // TODO: 实现注册逻辑
    res.json({
      status: 'success',
      message: '注册成功',
      data: {
        id: '1',
        username,
        email
      }
    })
  } catch (error) {
    res.status(500).json({
      status: 'error',
      message: '注册失败'
    })
  }
})

export default router
