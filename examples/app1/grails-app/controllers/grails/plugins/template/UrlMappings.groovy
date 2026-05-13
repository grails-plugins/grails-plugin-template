package grails.plugins.template

class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{}
        "/"(redirect:'/test')
    }
}
