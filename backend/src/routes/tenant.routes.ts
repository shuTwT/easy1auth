import { Router } from 'express'
import tenantController from '../controllers/tenant.controller'
import { authMiddleware } from '../middleware/auth'

const router = Router()

router.use(authMiddleware)

router.post('/', tenantController.create)

router.get('/', tenantController.getList)

router.get('/check-domain', tenantController.checkDomain)

router.get('/:id', tenantController.getById)

router.put('/:id', tenantController.update)

router.delete('/:id', tenantController.delete)

router.patch('/:id/status', tenantController.updateStatus)

router.get('/:id/stats', tenantController.getStats)

router.get('/:id/validate-limits', tenantController.validateLimits)

export default router
