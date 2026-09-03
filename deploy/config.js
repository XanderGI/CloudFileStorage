window.APP_CONFIG = {
    githubLink: "https://github.com/XanderGI/CloudFileStorage",

    mainName: "Cloud Storage",

    baseUrl: "",

    baseApi: "/api",

    validateLoginForm: true,
    validateRegistrationForm: true,

    validUsername: {
        minLength: 5,
        maxLength: 20,
        pattern: "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$",
    },

    validPassword: {
        minLength: 5,
        maxLength: 64,
        pattern: "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>[\\]/`~+=-_';]*$",
    },

    validFolderName: {
        minLength: 1,
        maxLength: 200,
        pattern: "^[^/\\\\:*?\"<>|]+$",
    },

    isMoveAllowed: true,

    isCutPasteAllowed: true,

    isFileContextMenuAllowed: true,

    isShortcutsAllowed: true,

    functions: {
        mapObjectToFrontFormat: (obj) => {
            const isDirectory = obj.type === "DIRECTORY";
            const name = isDirectory && !obj.name.endsWith("/") ? `${obj.name}/` : obj.name;

            return {
                lastModified: null,
                name: name,
                size: obj.size,
                path: obj.path + name,
                folder: isDirectory
            }
        },

    }

};
