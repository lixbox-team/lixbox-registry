import Vue from 'vue'
import Router from 'vue-router'
import Registry from '@/view/registry'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    { 
        path: '/', redirect: { name: 'registry' }},
    {
        path: '/registry', 
        name: 'registry', component: Registry
    }
  ]
})